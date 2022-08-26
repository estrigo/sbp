package kz.spt.reportplugin.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.lib.bootstrap.datatable.*;
import kz.spt.lib.extension.PluginRegister;
import kz.spt.lib.model.CarState;
import kz.spt.lib.model.dto.CarStateDto;
import kz.spt.lib.model.dto.CarStateFilterDto;
import kz.spt.lib.service.CarStateService;
import kz.spt.lib.service.EventLogService;
import kz.spt.lib.service.PluginService;
import kz.spt.lib.utils.StaticValues;
import kz.spt.reportplugin.ReportPlugin;
import kz.spt.reportplugin.datatable.JournalReportDtoComparators;
import kz.spt.reportplugin.dto.JournalReportDto;
import kz.spt.reportplugin.dto.filter.FilterJournalReportDto;
import kz.spt.reportplugin.dto.filter.FilterReportDto;
import kz.spt.reportplugin.service.ReportService;
import kz.spt.reportplugin.service.RootServicesGetterService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.apache.commons.lang3.ObjectUtils;
import org.jvnet.hk2.annotations.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Log
@Service
@RequiredArgsConstructor
public class JournalReportServiceImpl implements ReportService<JournalReportDto> {
    private static final Comparator<JournalReportDto> EMPTY_COMPARATOR = (e1, e2) -> 0;
    private ObjectMapper objectMapper = new ObjectMapper();

    private PluginService pluginService;
    private CarStateService carStateService;
    private RootServicesGetterService rootServicesGetterService;

    @SneakyThrows
    @Override
    public Page<JournalReportDto> list(PagingRequest pagingRequest, FilterReportDto filterReportDto) {

        Order order = pagingRequest.getOrder().get(0);
        int columnIndex = order.getColumn();
        pagingRequest.getColumns().get(columnIndex).setData("id");

        var filter = (FilterJournalReportDto) filterReportDto;
        var carStates = (Page<CarStateDto>) getCarStateService().getAll(pagingRequest, CarStateFilterDto.builder()
                .dateToString(filter.dateToString(filter.getDateTo()))
                .dateFromString(filter.dateToString(filter.getDateFrom()))
                .build());


        var result = new ArrayList<JournalReportDto>();
        for (var carState : carStates.getData()) {
            PluginRegister billingPluginRegister = getPluginService().getPluginRegister(StaticValues.billingPlugin);
            if (billingPluginRegister != null) {
                ObjectNode node = this.objectMapper.createObjectNode();
                node.put("command", "getPayments");
                node.put("carStateId", carState.getId());

                JsonNode paymentResult = billingPluginRegister.execute(node);
                ArrayNode payments = paymentResult.withArray("payments");
                if (payments.isEmpty()) {
                    result.add(JournalReportDto.builder()
                            .carStateId(carState.getId())
                            .carNumber(carState.getCarNumber())
                            .outTimestamp(carState.getOutTimestamp())
                            .inTimestamp(carState.getInTimestamp())
                            .parkingTypeCode(carState.getType() != null ? carState.getType().name() : "")
                            .paymentId(null)
                            .sum(BigDecimal.ZERO)
                            .provider("")
                            .cashlessPayment(false)
                            .build());
                } else {
                    payments.forEach(p -> {
                        result.add(JournalReportDto.builder()
                                .carStateId(carState.getId())
                                .carNumber(carState.getCarNumber())
                                .outTimestamp(carState.getOutTimestamp())
                                .inTimestamp(carState.getInTimestamp())
                                .parkingTypeCode(carState.getType().name())
                                .paymentId(p.get("paymentId").longValue())
                                .sum(p.get("sum").decimalValue())
                                .provider(p.get("provider").textValue())
                                .cashlessPayment(p.get("cashlessPayment").booleanValue())
                                .build());
                    });
                }
            } else {
                result.add(JournalReportDto.builder()
                        .carStateId(carState.getId())
                        .paymentId(null)
                        .carNumber(carState.getCarNumber())
                        .outTimestamp(carState.getOutTimestamp())
                        .inTimestamp(carState.getInTimestamp())
                        .parkingTypeCode(carState.getType().name())
                        .sum(BigDecimal.ZERO)
                        .provider("")
                        .cashlessPayment(false)
                        .build());
            }
        }

        Page<JournalReportDto> page = new Page<>(result);
        page.setRecordsFiltered(carStates.getRecordsFiltered());
        page.setRecordsTotal(carStates.getRecordsTotal());
        page.setDraw(pagingRequest.getDraw());

        return page;
    }

    @SneakyThrows
    @Override
    public List<JournalReportDto> list(FilterReportDto filterReportDto) {
        var filter = (FilterJournalReportDto) filterReportDto;
        var carStates = (List<CarState>) getCarStateService().listByFilters(CarStateFilterDto.builder()
                .dateToString(filter.dateToString(filter.getDateTo()))
                .dateFromString(filter.dateToString(filter.getDateFrom()))
                .build());
        List<Long> carStateIds = carStates.stream().map(CarState::getId).collect(Collectors.toList());
        PluginRegister billingPluginRegister = getPluginService().getPluginRegister(StaticValues.billingPlugin);
        var result = new ArrayList<JournalReportDto>();
        if (billingPluginRegister != null) {
            ObjectNode node = this.objectMapper.createObjectNode();
            node.put("command", "getPayments");
            ObjectMapper mapper = new ObjectMapper();
            ArrayNode array = mapper.valueToTree(carStateIds);
            node.set("carStateIds", array);
            JsonNode paymentResult = billingPluginRegister.execute(node);
            ArrayNode payments = paymentResult.withArray("payments");


            for (var carState : carStates) {
                JsonNode payment = getPaymentInfo(payments, carState.getId());
                if (ObjectUtils.isEmpty(payment)) {
                    result.add(JournalReportDto.builder()
                            .carStateId(carState.getId())
                            .carNumber(carState.getCarNumber())
                            .outTimestamp(carState.getOutTimestamp())
                            .inTimestamp(carState.getInTimestamp())
                            .parkingTypeCode(carState.getType() != null ? carState.getType().name() : "")
                            .paymentId(null)
                            .sum(BigDecimal.ZERO)
                            .provider("")
                            .cashlessPayment(false)
                            .build());
                } else {
                    result.add(JournalReportDto.builder()
                            .carStateId(carState.getId())
                            .carNumber(carState.getCarNumber())
                            .outTimestamp(carState.getOutTimestamp())
                            .inTimestamp(carState.getInTimestamp())
                            .parkingTypeCode(carState.getType().name())
                            .paymentId(payment.get("paymentId").longValue())
                            .sum(payment.get("sum").decimalValue())
                            .provider(payment.get("provider").textValue())
                            .cashlessPayment(payment.get("cashlessPayment").booleanValue())
                            .build());
                }
            }
        }

        return result;
    }

    private JsonNode getPaymentInfo (ArrayNode payments, Long carStateId){
        for (final JsonNode payment : payments) {
            if (payment.has("carStateId") && payment.get("carStateId").longValue() == carStateId) {
                return payment;
            }
        }
        return null;
    }

    @Override
    public Page<JournalReportDto> page(PagingRequest pagingRequest) {
        var all = list(pagingRequest, convert(pagingRequest));

        Page<JournalReportDto> page = new Page<>(all.getData());
        page.setRecordsFiltered(all.getRecordsFiltered());
        page.setRecordsTotal(all.getRecordsTotal());
        page.setDraw(pagingRequest.getDraw());

        return page;
    }

    @Override
    public Predicate<JournalReportDto> filterPage(PagingRequest pagingRequest) {
        return result -> true;
    }

    @Override
    public Comparator<JournalReportDto> sortPage(PagingRequest pagingRequest) {
        if (pagingRequest.getOrder() == null) {
            return EMPTY_COMPARATOR;
        }

        try {
            Order order = pagingRequest.getOrder().get(0);

            int columnIndex = order.getColumn();
            Column column = pagingRequest.getColumns().get(columnIndex);

            Comparator<JournalReportDto> comparator = JournalReportDtoComparators.getComparator(column.getData(), order.getDir());
            return Objects.requireNonNullElse(comparator, EMPTY_COMPARATOR);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return EMPTY_COMPARATOR;
    }

    private FilterJournalReportDto convert(PagingRequest pagingRequest) {
        return pagingRequest.convertTo(FilterJournalReportDto.builder().build());
    }

    private RootServicesGetterService getRootServicesGetterService() {
        if (rootServicesGetterService == null) {
            rootServicesGetterService = (RootServicesGetterService) ReportPlugin.INSTANCE.getApplicationContext().getBean("rootServicesGetterServiceImpl");
        }
        return rootServicesGetterService;
    }

    private CarStateService getCarStateService(){
        if(carStateService==null){
            carStateService = getRootServicesGetterService().getCarStateService();
        }

        if (carStateService == null) {
            carStateService = (CarStateService) ReportPlugin.INSTANCE.getMainApplicationContext().getBean("carStateServiceImpl");
        }

        return carStateService;
    }

    private PluginService getPluginService(){
        if(pluginService==null){
            pluginService = getRootServicesGetterService().getPluginService();
        }

        if (pluginService == null) {
            pluginService = (PluginService) ReportPlugin.INSTANCE.getMainApplicationContext().getBean("pluginServiceImpl");
        }

        return pluginService;
    }
}

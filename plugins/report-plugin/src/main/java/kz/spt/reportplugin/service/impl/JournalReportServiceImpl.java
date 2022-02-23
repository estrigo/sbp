package kz.spt.reportplugin.service.impl;

import kz.spt.billingplugin.model.Payment;
import kz.spt.billingplugin.service.PaymentService;
import kz.spt.lib.bootstrap.datatable.Column;
import kz.spt.lib.bootstrap.datatable.Order;
import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.lib.model.CarState;
import kz.spt.lib.model.dto.CarStateFilterDto;
import kz.spt.lib.service.CarStateService;
import kz.spt.reportplugin.datatable.JournalReportDtoComparators;
import kz.spt.reportplugin.dto.JournalReportDto;
import kz.spt.reportplugin.dto.filter.FilterJournalReportDto;
import kz.spt.reportplugin.dto.filter.FilterReportDto;
import kz.spt.reportplugin.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.jvnet.hk2.annotations.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JournalReportServiceImpl implements ReportService<JournalReportDto> {
    private static final Comparator<JournalReportDto> EMPTY_COMPARATOR = (e1, e2) -> 0;
    private final CarStateService carStateService;
    private final PaymentService paymentService;

    @Override
    public List<JournalReportDto> list(FilterReportDto filterReportDto) {
        var filter = (FilterJournalReportDto) filterReportDto;
        var carStates = (List<CarState>) carStateService.listByFilters(CarStateFilterDto.builder().build());

        var result = new ArrayList<JournalReportDto>();
        for (var carState : carStates) {
            var payments = (List<Payment>) paymentService.getPaymentsByCarStateId(carState.getId());
            if (payments.isEmpty()) {
                result.add(JournalReportDto.builder()
                        .carStateId(carState.getId())
                        .paymentId(null)
                        .carNumber(carState.getCarNumber())
                        .outTimestamp(carState.getOutTimestamp())
                        .inTimestamp(carState.getInTimestamp())
                        .parkingTypeCode(carState.getType().name())
                        .sum(null)
                        .provider("")
                        .build());
            } else {
                result.addAll(payments.stream()
                        .map(p -> JournalReportDto.builder()
                                .carStateId(carState.getId())
                                .paymentId(p.getId())
                                .carNumber(carState.getCarNumber())
                                .outTimestamp(carState.getOutTimestamp())
                                .inTimestamp(carState.getInTimestamp())
                                .parkingTypeCode(carState.getParking().getParkingType().name())
                                .sum(p.getPrice())
                                .provider(p.getProvider() != null ? p.getProvider().getName() : "")
                                .build())
                        .collect(Collectors.toList()));
            }
        }

        return result;
    }

    @Override
    public Page<JournalReportDto> page(PagingRequest pagingRequest) {
        var all = list(convert(pagingRequest));

        var filtered = all.stream()
                .filter(filterPage(pagingRequest))
                .sorted(sortPage(pagingRequest))
                .skip(pagingRequest.getStart())
                .limit(pagingRequest.getLength())
                .collect(Collectors.toList());

        long count = all.stream().filter(filterPage(pagingRequest)).count();

        Page<JournalReportDto> page = new Page<>(filtered);
        page.setRecordsFiltered((int) count);
        page.setRecordsTotal((int) count);
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
}

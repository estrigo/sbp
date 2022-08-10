package kz.spt.reportplugin.service.impl;

import kz.spt.lib.bootstrap.datatable.Column;
import kz.spt.lib.bootstrap.datatable.Order;
import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.lib.model.EventLog;
import kz.spt.lib.model.dto.EventsDto;
import kz.spt.lib.service.EventLogService;
import kz.spt.reportplugin.ReportPlugin;
import kz.spt.reportplugin.datatable.ManualOpenReportDtoComparators;
import kz.spt.reportplugin.dto.filter.FilterJournalReportDto;
import kz.spt.reportplugin.dto.filter.FilterReportDto;
import kz.spt.reportplugin.service.ReportService;
import kz.spt.reportplugin.service.RootServicesGetterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ManualOpenReportServiceImpl implements ReportService<EventsDto> {
    private static final Comparator<EventsDto> EMPTY_COMPARATOR = (e1, e2) -> 0;
    private EventLogService eventLogService;
    private RootServicesGetterService rootServicesGetterService;

    @Override
    public List<EventsDto> list(FilterReportDto filterReportDto) {
        var result = Stream.of((List<EventLog>) getEventLogService().listByType(EventLog.EventType.MANUAL_GATE_CLOSE), (List<EventLog>) eventLogService.listByType(EventLog.EventType.MANUAL_GATE_OPEN))
                .flatMap(m -> m.stream()
                        .map(e -> EventsDto.builder()
                                .id(e.getId())
                                .created(e.getCreated())
                                .plateNumber(e.getNullSafePlateNumber())
                                .description(e.getNullSafeDescription())
                                .descriptionEn(e.getNullSafeDescriptionEn())
                                .smallImgUrl(e.getProperties().get("carSmallImageUrl") != null ? (String) e.getProperties().get("carSmallImageUrl") : "")
                                .bigImgUrl(e.getProperties().get("carImageUrl") != null ? (String) e.getProperties().get("carImageUrl") : "")
                                .build()))
                .collect(Collectors.toList());
        return result;
    }

    @Override
    public Page<EventsDto> list(PagingRequest pagingRequest, FilterReportDto filterReportDto) {
        return null;
    }

    @Override
    public Page<EventsDto> page(PagingRequest pagingRequest) {
        var all = list(FilterJournalReportDto.builder().build());

        var filtered = all.stream()
                .filter(filterPage(pagingRequest))
                .sorted(sortPage(pagingRequest))
                .skip(pagingRequest.getStart())
                .limit(pagingRequest.getLength())
                .collect(Collectors.toList());

        long count = all.stream().filter(filterPage(pagingRequest)).count();

        Page<EventsDto> page = new Page<>(filtered);
        page.setRecordsFiltered((int) count);
        page.setRecordsTotal((int) count);
        page.setDraw(pagingRequest.getDraw());

        return page;
    }

    @Override
    public Predicate<EventsDto> filterPage(PagingRequest pagingRequest) {
        return result -> true;
    }

    @Override
    public Comparator<EventsDto> sortPage(PagingRequest pagingRequest) {
        if (pagingRequest.getOrder() == null) {
            return EMPTY_COMPARATOR;
        }

        try {
            Order order = pagingRequest.getOrder().get(0);

            int columnIndex = order.getColumn();
            Column column = pagingRequest.getColumns().get(columnIndex);

            Comparator<EventsDto> comparator = ManualOpenReportDtoComparators.getComparator(column.getData(), order.getDir());
            return Objects.requireNonNullElse(comparator, EMPTY_COMPARATOR);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return EMPTY_COMPARATOR;
    }

    private RootServicesGetterService getRootServicesGetterService() {
        if (rootServicesGetterService == null) {
            rootServicesGetterService = (RootServicesGetterService) ReportPlugin.INSTANCE.getApplicationContext().getBean("rootServicesGetterServiceImpl");
        }
        return rootServicesGetterService;
    }

    private EventLogService getEventLogService(){
        if(eventLogService==null){
            eventLogService = getRootServicesGetterService().getEventLogService();
        }

        if (eventLogService == null) {
            eventLogService = (EventLogService) ReportPlugin.INSTANCE.getMainApplicationContext().getBean("eventLogServiceImpl");
        }

        return eventLogService;
    }
}

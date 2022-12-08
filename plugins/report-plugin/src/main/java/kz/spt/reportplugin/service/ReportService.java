package kz.spt.reportplugin.service;

import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.lib.model.dto.EventFilterDto;
import kz.spt.lib.model.dto.EventsDto;
import kz.spt.reportplugin.dto.filter.FilterReportDto;

import java.text.ParseException;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public interface ReportService<T> {
    List<T> list(FilterReportDto filterReportDto);
    Page<T> list(PagingRequest pagingRequest, FilterReportDto filterReportDto);
    Page<T> page(PagingRequest pagingRequest);
    Page<T> pageFilter(PagingRequest pagingRequest, EventFilterDto eventFilterDto) throws ParseException;
    Predicate<T> filterPage(PagingRequest pagingRequest);
    Comparator<T> sortPage(PagingRequest pagingRequest);
}

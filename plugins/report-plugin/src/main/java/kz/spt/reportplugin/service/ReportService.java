package kz.spt.reportplugin.service;

import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.reportplugin.dto.filter.FilterReportDto;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public interface ReportService<T> {
    List<T> list(FilterReportDto filterReportDto);
    Page<T> list(PagingRequest pagingRequest, FilterReportDto filterReportDto);
    Page<T> page(PagingRequest pagingRequest);
    Predicate<T> filterPage(PagingRequest pagingRequest);
    Comparator<T> sortPage(PagingRequest pagingRequest);
}

package kz.spt.reportplugin.rest;

import kz.spt.reportplugin.service.ReportService;

public abstract class BasicRestController<T> {
    public ReportService<T> reportService;

    public BasicRestController(ReportService<T> reportService){
        this.reportService = reportService;
    }
}

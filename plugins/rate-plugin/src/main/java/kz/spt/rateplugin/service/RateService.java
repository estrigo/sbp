package kz.spt.rateplugin.service;

import java.util.Date;

public interface RateService {

    int calculatePayment(Long parkingId, Date inDate, Date outDate);
}

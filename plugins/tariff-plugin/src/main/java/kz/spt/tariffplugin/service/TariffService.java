package kz.spt.tariffplugin.service;

import java.util.Date;

public interface TariffService {

    int calculatePayment(Long parkingId, Date inDate, Date outDate);
}

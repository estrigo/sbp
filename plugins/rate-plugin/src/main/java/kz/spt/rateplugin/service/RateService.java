package kz.spt.rateplugin.service;

import kz.spt.api.model.Parking;
import kz.spt.rateplugin.model.ParkingRate;
import kz.spt.rateplugin.model.dto.ParkingRateDto;

import java.util.Date;
import java.util.List;

public interface RateService {

    ParkingRate getById(Long id);

    int calculatePayment(Long parkingId, Date inDate, Date outDate);

    List<ParkingRateDto> listPaymentParkings();

    void saveRate(ParkingRate rate);

    Parking getParkingById(Long parkingId);
}
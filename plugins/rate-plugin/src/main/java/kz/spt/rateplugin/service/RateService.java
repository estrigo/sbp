package kz.spt.rateplugin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import kz.spt.lib.model.Parking;
import kz.spt.rateplugin.model.ParkingRate;
import kz.spt.rateplugin.model.dto.ParkingRateDto;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public interface RateService {

    ParkingRate getById(Long id);

    ParkingRate getByParkingId(Long parkingId);

    BigDecimal calculatePayment(Long parkingId, Date inDate, Date outDate, Boolean cashlessPayment, Boolean isCheck, String payments, String carType, String plateNumber) throws JsonProcessingException;

    int calculateFreeMinutes(Long parkingId, Date inDate, Date outDate, String payments);

    List<ParkingRateDto> listPaymentParkings();

    void saveRate(ParkingRate rate);

    Parking getParkingById(Long parkingId);
}

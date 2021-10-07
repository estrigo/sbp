package kz.spt.rateplugin.service.impl;

import kz.spt.lib.model.Parking;
import kz.spt.lib.service.ParkingService;
import kz.spt.rateplugin.RatePlugin;
import kz.spt.rateplugin.repository.ParkingRepository;
import kz.spt.rateplugin.model.ParkingRate;
import kz.spt.rateplugin.model.dto.ParkingRateDto;
import kz.spt.rateplugin.repository.RateRepository;
import kz.spt.rateplugin.service.RateService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class RateServiceImpl implements RateService {

    private RateRepository rateRepository;
    private ParkingRepository parkingRepository;
    private ParkingService parkingService;

    public RateServiceImpl(RateRepository rateRepository, ParkingRepository parkingRepository){
        this.rateRepository = rateRepository;
        this.parkingRepository = parkingRepository;
    }

    @Override
    public ParkingRate getById(Long id) {
        return rateRepository.getWithParking(id);
    }

    @Override
    public ParkingRate getByParkingId(Long parkingId) {
        return rateRepository.getByParkingId(parkingId);
    }

    @Override
    public BigDecimal calculatePayment(Long parkingId, Date inDate, Date outDate) {

        ParkingRate parkingRate = getByParkingId(parkingId);

        Calendar inCalendar = Calendar.getInstance();
        inCalendar.setTime(inDate);
        inCalendar.add(Calendar.MINUTE, parkingRate.getBeforeFreeMinutes());

        Calendar outCalendar = Calendar.getInstance();
        outCalendar.setTime(outDate);

        if(inCalendar.after(outCalendar)){
            return BigDecimal.ZERO;
        }
        int hours = 0;
        while (inCalendar.before(outCalendar)){
            hours++;
            inCalendar.add(Calendar.HOUR, 1);
        }

        return BigDecimal.valueOf(parkingRate.getOnlinePaymentValue()).multiply(BigDecimal.valueOf(hours));
    }

    @Override
    public List<ParkingRateDto> listPaymentParkings() {
        List<ParkingRateDto> list = new ArrayList<>();

        List<ParkingRate> rates = rateRepository.findAll();
        List<Parking> paymentParkings = parkingRepository.listPaymentParkings();

        for(Parking parking: paymentParkings){
            ParkingRateDto dto = new ParkingRateDto();
            dto.parking = parking;
            for(ParkingRate parkingRate: rates){
                if(parkingRate.getParking()!=null && parking.getId().equals(parkingRate.getParking().getId())){
                    dto.parkingRate = parkingRate;
                }
            }
            list.add(dto);
        }
        return list;
    }

    @Override
    public void saveRate(ParkingRate rate) {
        rateRepository.save(rate);
    }

    @Override
    public Parking getParkingById(Long parkingId) {
        return getParkingService().findById(parkingId);
    }

    private ParkingService getParkingService(){
        if(this.parkingService == null){
            parkingService = (ParkingService) RatePlugin.INSTANCE.getMainApplicationContext().getBean("parkingServiceImpl");
        }
        return parkingService;
    }
}

package kz.spt.rateplugin.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
import java.util.*;

@Service
public class RateServiceImpl implements RateService {

    private RateRepository rateRepository;
    private ParkingRepository parkingRepository;
    private ParkingService parkingService;
    private static ObjectMapper mapper = new ObjectMapper();

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
    public BigDecimal calculatePayment(Long parkingId, Date inDate, Date outDate, Boolean cashlessPayment) throws JsonProcessingException {

        ParkingRate parkingRate = getByParkingId(parkingId);

        Calendar inCalendar = Calendar.getInstance();
        inCalendar.setTime(inDate);
        inCalendar.add(Calendar.MINUTE, parkingRate.getBeforeFreeMinutes());

        Calendar outCalendar = Calendar.getInstance();
        outCalendar.setTime(outDate);

        if(inCalendar.after(outCalendar)){
            return BigDecimal.ZERO;
        }

        if(ParkingRate.RateType.PROGRESSIVE.equals(parkingRate.getRateType())){
            ArrayNode progressiveJson = (ArrayNode) mapper.readTree(parkingRate.getProgressiveJson());
            Map<Integer, Integer> prices = new HashMap<>();
            Iterator<JsonNode> iterator = progressiveJson.iterator();
            while (iterator.hasNext()) {
                JsonNode node = iterator.next();
                prices.put(node.get("hour").intValue(), Integer.valueOf(node.get("value").textValue()));
            }

            BigDecimal result = BigDecimal.ZERO;
            int hours = 0;

            while (inCalendar.before(outCalendar)){
                hours++;
                if(hours < prices.size()){
                    result = result.add(BigDecimal.valueOf(prices.get(hours)));
                } else {
                    result = result.add(BigDecimal.valueOf(prices.get(prices.size())));
                }
                inCalendar.add(Calendar.HOUR, 1);
            }
            return result;
        } else {
            int hours = 0;
            while (inCalendar.before(outCalendar)){
                hours++;
                inCalendar.add(Calendar.HOUR, 1);
            }
            return BigDecimal.valueOf(cashlessPayment ? parkingRate.getOnlinePaymentValue() : parkingRate.getCashPaymentValue()).multiply(BigDecimal.valueOf(hours));
        }
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

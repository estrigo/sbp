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
import lombok.extern.java.Log;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Log
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

        if(!inCalendar.before(outCalendar)){
            return BigDecimal.ZERO;
        }
        inCalendar.add(Calendar.MINUTE, (-1) * parkingRate.getBeforeFreeMinutes());

        BigDecimal result = BigDecimal.ZERO;

        Calendar inDayCalendar = Calendar.getInstance();
        inDayCalendar.setTime(inCalendar.getTime());
        inDayCalendar.add(Calendar.DATE, 1);

        if(parkingRate.getDayPaymentValue() != null && inDayCalendar.before(outCalendar)){
            while (inDayCalendar.before(outCalendar)){
                result = result.add(BigDecimal.valueOf(parkingRate.getDayPaymentValue()));
                inDayCalendar.add(Calendar.DATE, 1);
            }
            inDayCalendar.add(Calendar.DATE, -1);
            inCalendar.setTime(inDayCalendar.getTime());
        }

        if(ParkingRate.RateType.PROGRESSIVE.equals(parkingRate.getRateType())){
            ArrayNode progressiveJson = (ArrayNode) mapper.readTree(parkingRate.getProgressiveJson());
            Map<Integer, Integer> onlinePrices = new HashMap<>();
            Map<Integer, Integer> parkomatPrices = new HashMap<>();
            Iterator<JsonNode> iterator = progressiveJson.iterator();
            while (iterator.hasNext()) {
                JsonNode node = iterator.next();
                onlinePrices.put(node.get("hour").intValue(), Integer.valueOf(node.get("onlineValue").textValue()));
                parkomatPrices.put(node.get("hour").intValue(), Integer.valueOf(node.get("parkomatValue").textValue()));
            }

            int hours = 0;

            while (inCalendar.before(outCalendar)){
                hours++;
                if(hours < onlinePrices.size()){
                    if(cashlessPayment){
                        result = result.add(BigDecimal.valueOf(onlinePrices.get(hours)));
                    } else {
                        result = result.add(BigDecimal.valueOf(parkomatPrices.get(hours)));
                    }
                } else {
                    if(cashlessPayment){
                        result = result.add(BigDecimal.valueOf(onlinePrices.get(onlinePrices.size())));
                    } else {
                        result = result.add(BigDecimal.valueOf(parkomatPrices.get(parkomatPrices.size())));
                    }
                }
                inCalendar.add(Calendar.HOUR, 1);
            }
            return result;
        } else if(ParkingRate.RateType.INTERVAL.equals(parkingRate.getRateType())){

            ArrayNode intervalJson = (ArrayNode) mapper.readTree(parkingRate.getIntervalJson());

            int inCalendarHour = inCalendar.get(Calendar.HOUR_OF_DAY);
            JsonNode current = getSatisfiedJsonNode(inCalendarHour, intervalJson);
            ArrayNode conditionJson = (ArrayNode) current.get("condition");
            Iterator<JsonNode> conditionIterator = conditionJson.iterator();
            int intervalFrom = Integer.valueOf(current.get("intervalFrom").textValue());
            int intervalTo = Integer.valueOf(current.get("intervalTo").textValue());
            String intervalType = null;
            Integer intervalOnlineHours = 0;
            Integer intervalParkomatHours = 0;
            while (inCalendar.before(outCalendar)){
                inCalendarHour = inCalendar.get(Calendar.HOUR_OF_DAY);
                if(intervalFrom == intervalTo || (intervalFrom < intervalTo && inCalendarHour >= intervalFrom && inCalendarHour < intervalTo) || (intervalFrom > intervalTo && (inCalendarHour >= intervalFrom || inCalendarHour < intervalTo))){
                    log.info("inCalendarHour inside interval");
                } else {
                    current = getSatisfiedJsonNode(inCalendarHour, intervalJson);
                    conditionJson = (ArrayNode) current.get("condition");
                    conditionIterator = conditionJson.iterator();
                    intervalFrom = Integer.valueOf(current.get("intervalFrom").textValue());
                    intervalTo = Integer.valueOf(current.get("intervalTo").textValue());
                    intervalType = null;
                }

                if(intervalType == null || (!"entrance".equals(intervalType) && !"allNext".equals(intervalType))){
                    if (conditionIterator.hasNext()) {
                        JsonNode conditionNode = conditionIterator.next();
                        intervalType = conditionNode.get("intervalType").textValue();
                        intervalOnlineHours = Integer.valueOf(conditionNode.get("intervalOnlineHours").textValue());
                        intervalParkomatHours = Integer.valueOf(conditionNode.get("intervalParkomatHours").textValue());
                    }
                }
                if(cashlessPayment){
                    result = result.add(BigDecimal.valueOf(intervalOnlineHours));
                } else {
                    result = result.add(BigDecimal.valueOf(intervalParkomatHours));
                }

                Calendar nextInCalendar = Calendar.getInstance();
                nextInCalendar.setTime(inCalendar.getTime());
                nextInCalendar.add(Calendar.HOUR, 1);

                Calendar checkIntervalBorder = Calendar.getInstance();

                checkIntervalBorder.set(Calendar.HOUR_OF_DAY, intervalTo);
                checkIntervalBorder.set(Calendar.MINUTE, 0);
                checkIntervalBorder.set(Calendar.SECOND, 0);
                checkIntervalBorder.set(Calendar.MILLISECOND, 0);

                if(intervalFrom > intervalTo && inCalendar.get(Calendar.HOUR_OF_DAY) >= intervalFrom){
                    checkIntervalBorder.add(Calendar.DATE, 1);
                }
                if(intervalFrom != intervalTo && nextInCalendar.after(checkIntervalBorder)){
                    inCalendar.setTime(checkIntervalBorder.getTime());
                } else {
                    inCalendar.add(Calendar.HOUR, 1);
                }
            }

            return result;
        }
        else {
            int hours = 0;
            while (inCalendar.before(outCalendar)){
                hours++;
                inCalendar.add(Calendar.HOUR, 1);
            }
            result = result.add(BigDecimal.valueOf(cashlessPayment ? parkingRate.getOnlinePaymentValue() : parkingRate.getCashPaymentValue()).multiply(BigDecimal.valueOf(hours)));
            return result;
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

    private JsonNode getSatisfiedJsonNode(int inCalendarHour, ArrayNode intervalJson){
        Iterator<JsonNode> iterator = intervalJson.iterator();
        JsonNode returnNode = null;
        while (iterator.hasNext()) {
            JsonNode node = iterator.next();
            int intervalFrom = Integer.valueOf(node.get("intervalFrom").textValue());
            int intervalTo = Integer.valueOf(node.get("intervalTo").textValue());
            if(intervalFrom == intervalTo){ // 24 hours
                returnNode = node;
            } else if(intervalFrom < intervalTo && inCalendarHour >= intervalFrom && inCalendarHour < intervalTo){
                returnNode = node;
            } else if(intervalFrom > intervalTo && (inCalendarHour >= intervalFrom || inCalendarHour < intervalTo)){
                returnNode = node;
            }
        }
        return returnNode;
    }

    private ParkingService getParkingService(){
        if(this.parkingService == null){
            parkingService = (ParkingService) RatePlugin.INSTANCE.getMainApplicationContext().getBean("parkingServiceImpl");
        }
        return parkingService;
    }
}

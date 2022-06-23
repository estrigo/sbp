package kz.spt.rateplugin.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import kz.spt.lib.model.Parking;
import kz.spt.lib.service.ParkingService;
import kz.spt.lib.utils.StaticValues;
import kz.spt.rateplugin.RatePlugin;
import kz.spt.rateplugin.repository.ParkingRepository;
import kz.spt.rateplugin.model.ParkingRate;
import kz.spt.rateplugin.model.dto.ParkingRateDto;
import kz.spt.rateplugin.repository.RateRepository;
import kz.spt.rateplugin.service.RateService;
import lombok.extern.java.Log;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

@Log
@Service
public class RateServiceImpl implements RateService {

    private RateRepository rateRepository;
    private ParkingRepository parkingRepository;
    private ParkingService parkingService;
    private static ObjectMapper mapper = new ObjectMapper();

    public RateServiceImpl(RateRepository rateRepository, ParkingRepository parkingRepository) {
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
    public BigDecimal calculatePayment(Long parkingId, Date inDate, Date outDate, Boolean cashlessPayment, Boolean isCheck, String paymentsJson, String carType) throws JsonProcessingException {

        ParkingRate parkingRate = getByParkingId(parkingId);

        Calendar inCalendar = Calendar.getInstance();
        inCalendar.setTime(inDate);

        Calendar outCalendar = Calendar.getInstance();
        outCalendar.setTime(outDate);

        if(!isCheck){
            inCalendar.add(Calendar.MINUTE, parkingRate.getBeforeFreeMinutes());
        }

        if (!inCalendar.before(outCalendar)) {
            return BigDecimal.ZERO;
        }

        if(parkingRate != null && ParkingRate.RateType.PREPAID.equals(parkingRate.getRateType())){
            return parkingRate.getPrepaidValue() != null ? BigDecimal.valueOf(parkingRate.getPrepaidValue()) : BigDecimal.ZERO;
        }

        if(!isCheck){
            inCalendar.add(Calendar.MINUTE, (-1) * parkingRate.getBeforeFreeMinutes());
        }

        BigDecimal result = BigDecimal.ZERO;

        int outDateToCurrentDateDiffSeconds = (int) ((new Date()).getTime() - outDate.getTime()) / 1000;

        if(parkingRate != null && parkingRate.getAfterFreeMinutes() != null && outDateToCurrentDateDiffSeconds < parkingRate.getAfterFreeMinutes() * 60){
            log.info("parkingRate.getAfterFreeMinutes(): " +  parkingRate.getAfterFreeMinutes());
            Date lastPaymentDate = getLastPaymentDate(paymentsJson); // Если была оплата проверяем прошли минуты до которые даются для выезда
            if(lastPaymentDate != null){
                log.info("lastPaymentDate: " +  lastPaymentDate);
                int seconds = (int) ((new Date()).getTime() - lastPaymentDate.getTime()) / 1000;
                int minutesPassedAfterLastPay = seconds / 60;
                int secondsPassedAfterLastPay = seconds % 60;
                log.info("minutesPassedAfterLastPay: " + minutesPassedAfterLastPay);
                log.info("secondsPassedAfterLastPay: " + secondsPassedAfterLastPay);
                if (minutesPassedAfterLastPay < parkingRate.getAfterFreeMinutes()) {
                    outCalendar.add(Calendar.MINUTE, (-1) * minutesPassedAfterLastPay);
                    outCalendar.add(Calendar.SECOND, (-1) * secondsPassedAfterLastPay);
                }
            }
        }

        Calendar inDayCalendar = Calendar.getInstance();
        inDayCalendar.setTime(inCalendar.getTime());
        inDayCalendar.add(Calendar.DATE, 1);

        if (parkingRate != null && parkingRate.getDayPaymentValue() != null && inDayCalendar.before(outCalendar)) {
            if(parkingRate.getMoreHoursCalcInDays()!=null && parkingRate.getMoreHoursCalcInDays()){
                Calendar tmpInDayCalendar = Calendar.getInstance();
                tmpInDayCalendar.setTime(inDayCalendar.getTime());

                while (tmpInDayCalendar.before(outCalendar)) {
                    result = result.add(BigDecimal.valueOf(parkingRate.getDayPaymentValue()));
                    tmpInDayCalendar.add(Calendar.DATE, 1);
                }
                inDayCalendar.add(Calendar.DATE, -1);
                outCalendar.setTime(tmpInDayCalendar.getTime());
            } else {
                while (inDayCalendar.before(outCalendar)) {
                    result = result.add(BigDecimal.valueOf(parkingRate.getDayPaymentValue()));
                    inDayCalendar.add(Calendar.DATE, 1);
                }
                inDayCalendar.add(Calendar.DATE, -1);
                inCalendar.setTime(inDayCalendar.getTime());
            }
        }

        if (parkingRate != null && ParkingRate.RateType.PROGRESSIVE.equals(parkingRate.getRateType())) {
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

            while (inCalendar.before(outCalendar)) {
                hours++;
                if (hours < onlinePrices.size()) {
                    if (cashlessPayment) {
                        result = result.add(BigDecimal.valueOf(onlinePrices.get(hours)));
                    } else {
                        result = result.add(BigDecimal.valueOf(parkomatPrices.get(hours)));
                    }
                } else {
                    if (cashlessPayment) {
                        result = result.add(BigDecimal.valueOf(onlinePrices.get(onlinePrices.size())));
                    } else {
                        result = result.add(BigDecimal.valueOf(parkomatPrices.get(parkomatPrices.size())));
                    }
                }
                inCalendar.add(Calendar.HOUR, 1);
            }
            return result;
        } else if (parkingRate != null && ParkingRate.RateType.INTERVAL.equals(parkingRate.getRateType())) {

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
            while (inCalendar.before(outCalendar)) {
                inCalendarHour = inCalendar.get(Calendar.HOUR_OF_DAY);
                if (intervalFrom == intervalTo || (intervalFrom < intervalTo && inCalendarHour >= intervalFrom && inCalendarHour < intervalTo) || (intervalFrom > intervalTo && (inCalendarHour >= intervalFrom || inCalendarHour < intervalTo))) {

                } else {
                    current = getSatisfiedJsonNode(inCalendarHour, intervalJson);
                    conditionJson = (ArrayNode) current.get("condition");
                    conditionIterator = conditionJson.iterator();
                    intervalFrom = Integer.valueOf(current.get("intervalFrom").textValue());
                    intervalTo = Integer.valueOf(current.get("intervalTo").textValue());
                    intervalType = null;
                }

                if (intervalType == null || (!"entrance".equals(intervalType) && !"allNext".equals(intervalType))) {
                    if (conditionIterator.hasNext()) {
                        JsonNode conditionNode = conditionIterator.next();
                        intervalType = conditionNode.get("intervalType").textValue();
                        intervalOnlineHours = Integer.valueOf(conditionNode.get("intervalOnlineHours").textValue());
                        intervalParkomatHours = Integer.valueOf(conditionNode.get("intervalParkomatHours").textValue());
                    }
                }
                else if (intervalType.equals("entrance")) {
                    intervalOnlineHours = 0;
                    intervalParkomatHours = 0;
                }
                if (cashlessPayment) {
                    result = result.add(BigDecimal.valueOf(intervalOnlineHours));
                } else {
                    result = result.add(BigDecimal.valueOf(intervalParkomatHours));
                }
                inCalendar.add(Calendar.HOUR, 1);
            }

            return result;
        } else if (parkingRate != null && ParkingRate.RateType.DIMENSIONS.equals(parkingRate.getRateType())) {
            int hours = 0;
            int nightHours = 0;

            while (inCalendar.before(outCalendar)) {
                int inCalendarHour = inCalendar.get(Calendar.HOUR_OF_DAY);
                int intervalFrom = 22;
                int intervalTo = 7;
                if (intervalFrom == intervalTo || (intervalFrom < intervalTo && inCalendarHour >= intervalFrom && inCalendarHour < intervalTo) || (intervalFrom > intervalTo && (inCalendarHour >= intervalFrom || inCalendarHour < intervalTo))) {
                    nightHours++;
                    log.info("Night Time Dimension ----- " + nightHours);
                } else {
                    hours++;
                    log.info("Time Dimension ----- " + hours);
                }
                inCalendar.add(Calendar.HOUR, 1);

            }
            log.info("it checks dimensions for car model" + carType);

            if (hours > 0) {

                if (!carType.equals("")) {
                    if (parkingRate != null && carType.equals("1")) {
                        result = result.add(BigDecimal.valueOf(cashlessPayment ? parkingRate.getOnlinePaymentValuePassenger() : parkingRate.getCashPaymentValuePassenger()).multiply(BigDecimal.valueOf(1)));
                    } else if (parkingRate != null && carType.equals("2")) {
                        result = result.add(BigDecimal.valueOf(cashlessPayment ? parkingRate.getOnlinePaymentValueVan() : parkingRate.getCashPaymentValueVan()).multiply(BigDecimal.valueOf(1)));
                    } else {
                        result = result.add(BigDecimal.valueOf(cashlessPayment ? parkingRate.getOnlinePaymentValueTruck() : parkingRate.getCashPaymentValueTruck()).multiply(BigDecimal.valueOf(1)));
                    }
                } else {
                    result = result.add(BigDecimal.valueOf(cashlessPayment ? parkingRate.getOnlinePaymentValuePassenger() : parkingRate.getCashPaymentValuePassenger()).multiply(BigDecimal.valueOf(1)));
                }
            }

            if (nightHours > 0) {
                log.info("night hours " + nightHours);
                if (!carType.equals("")) {
                    if (parkingRate != null && carType.equals("1")) {
                        result = result.add(BigDecimal.valueOf(cashlessPayment ? parkingRate.getOnlinePaymentValuePassengerNight() : parkingRate.getCashPaymentValuePassengerNight()).multiply(BigDecimal.valueOf(nightHours)));
                    } else if (parkingRate != null && carType.equals("2")) {
                        result = result.add(BigDecimal.valueOf(cashlessPayment ? parkingRate.getOnlinePaymentValueVanNight() : parkingRate.getCashPaymentValueVanNight()).multiply(BigDecimal.valueOf(nightHours)));
                    } else {
                        result = result.add(BigDecimal.valueOf(cashlessPayment ? parkingRate.getOnlinePaymentValueTruckNight() : parkingRate.getCashPaymentValueTruckNight()).multiply(BigDecimal.valueOf(nightHours)));
                    }
                }
                else {
                    result = result.add(BigDecimal.valueOf(cashlessPayment ? parkingRate.getOnlinePaymentValuePassengerNight() : parkingRate.getCashPaymentValuePassengerNight()).multiply(BigDecimal.valueOf(nightHours)));
                }
            }

            return result;
            }

        else {
            int hours = 0;
            while (inCalendar.before(outCalendar)) {
                hours++;
                inCalendar.add(Calendar.HOUR, 1);
            }
            if (parkingRate != null) {
                result = result.add(BigDecimal.valueOf(cashlessPayment ? parkingRate.getOnlinePaymentValue() : parkingRate.getCashPaymentValue()).multiply(BigDecimal.valueOf(hours)));
            }
            return result;
        }
    }

    private Calendar convertToCalendarTime(String time) throws Exception {
        Date time1 = new SimpleDateFormat("HH:mm:ss").parse(time);
        Calendar calendar1 = Calendar.getInstance();
        calendar1.setTime(time1);
        calendar1.add(Calendar.DATE, 1);
        return calendar1;
    }


    private Date getLastPaymentDate(String paymentsJson) {
        Date lastPaymentDate = null;
        if(paymentsJson != null && !"".equals(paymentsJson)){
            log.info("paymentsJson: " +  paymentsJson);
            try {
                SimpleDateFormat format = new SimpleDateFormat(StaticValues.dateFormat);
                ArrayNode payments = (ArrayNode) mapper.readTree(paymentsJson);
                Iterator<JsonNode> iterator = payments.iterator();
                while (iterator.hasNext()) {
                    JsonNode node = iterator.next();
                    Date created = format.parse(node.get("created").textValue());
                    if(lastPaymentDate == null || lastPaymentDate.before(created)){
                        lastPaymentDate = created;
                    }
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        return lastPaymentDate;
    }

    @Override
    public int calculateFreeMinutes(Long parkingId, Date inDate, Date outDate, String payments) {
        ParkingRate parkingRate = getByParkingId(parkingId);
        int freeMinutes = 0;
        if (parkingRate != null) {
            Calendar inCalendar = Calendar.getInstance();
            inCalendar.setTime(inDate);
            inCalendar.add(Calendar.MINUTE, parkingRate.getBeforeFreeMinutes());

            Calendar outCalendar = Calendar.getInstance();
            outCalendar.setTime(outDate);

            if (outCalendar.getTime().before(inCalendar.getTime())) { // Еще не истекли время бесплатных минут
                inCalendar.add(Calendar.MINUTE, (-1) * parkingRate.getBeforeFreeMinutes());
                int seconds = (int) (outDate.getTime() - inDate.getTime()) / 1000;
                int minutesPassed = seconds / 60;

                freeMinutes = parkingRate.getBeforeFreeMinutes() - minutesPassed - 1;
            } else {
                Date lastPaymentDate = getLastPaymentDate(payments);
                if (lastPaymentDate != null) {
                    int seconds = (int) (outCalendar.getTime().getTime() - lastPaymentDate.getTime()) / 1000;
                    int minutesPassedAfterLastPay = seconds / 60;
                    if (minutesPassedAfterLastPay < parkingRate.getAfterFreeMinutes()) {
                        freeMinutes = parkingRate.getAfterFreeMinutes() - minutesPassedAfterLastPay - 1;
                    }
                }
            }
        }
        return freeMinutes;
    }

    @Override
    public List<ParkingRateDto> listPaymentParkings() {
        List<ParkingRate> rates = rateRepository.findAll();
        List<Parking> paymentParkings = parkingRepository.listPaymentParkings();
        List<ParkingRateDto> list = new ArrayList<>(paymentParkings.size());

        for (Parking parking : paymentParkings) {
            ParkingRateDto dto = new ParkingRateDto();
            dto.parking = parking;
            for (ParkingRate parkingRate : rates) {
                if (parkingRate.getParking() != null && parking.getId().equals(parkingRate.getParking().getId())) {
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

    private JsonNode getSatisfiedJsonNode(int inCalendarHour, ArrayNode intervalJson) {
        Iterator<JsonNode> iterator = intervalJson.iterator();
        JsonNode returnNode = null;
        while (iterator.hasNext()) {
            JsonNode node = iterator.next();
            int intervalFrom = Integer.valueOf(node.get("intervalFrom").textValue());
            int intervalTo = Integer.valueOf(node.get("intervalTo").textValue());
            if (intervalFrom == intervalTo) { // 24 hours
                returnNode = node;
            } else if (intervalFrom < intervalTo && inCalendarHour >= intervalFrom && inCalendarHour < intervalTo) {
                returnNode = node;
            } else if (intervalFrom > intervalTo && (inCalendarHour >= intervalFrom || inCalendarHour < intervalTo)) {
                returnNode = node;
            }
        }
        return returnNode;
    }

    private ParkingService getParkingService() {
        if (this.parkingService == null) {
            parkingService = (ParkingService) RatePlugin.INSTANCE.getMainApplicationContext().getBean("parkingServiceImpl");
        }
        return parkingService;
    }
}

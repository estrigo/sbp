package kz.spt.app.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.app.model.dto.Period;
import kz.spt.lib.extension.PluginRegister;
import kz.spt.lib.model.CarState;
import kz.spt.lib.service.AbonomentService;
import kz.spt.lib.service.PluginService;
import kz.spt.lib.utils.StaticValues;
import lombok.extern.java.Log;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;

import static kz.spt.lib.utils.StaticValues.abonomentPlugin;

@Log
@Service
public class AbonomentServiceImpl implements AbonomentService {

    private final PluginService pluginService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AbonomentServiceImpl(PluginService pluginService){
        this.pluginService = pluginService;
    }

    @Override
    public JsonNode createAbonomentType(int period,String customJson, String type, int price) throws Exception {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("result", false);

        PluginRegister abonomentPluginRegister = pluginService.getPluginRegister(abonomentPlugin);
        if(abonomentPluginRegister != null){
            ObjectNode command = objectMapper.createObjectNode();
            command.put("command", "createType");
            command.put("period", period);
            command.put("customJson", customJson);
            command.put("type", type);
            command.put("price", price);
            JsonNode abonomentResult = abonomentPluginRegister.execute(command);
            result.put("result", abonomentResult.get("result").booleanValue());
            if(abonomentResult.has("error")){
                result.put("error", abonomentResult.get("error").textValue());
            }
        } else {
            result.put("error", "abonomentPluginNotStarted");
        }
        return result;
    }

    @Override
    public JsonNode deleteAbonomentType(Long id) throws Exception {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("result", false);

        PluginRegister abonomentPluginRegister = pluginService.getPluginRegister(abonomentPlugin);
        if(abonomentPluginRegister != null){
            ObjectNode command = objectMapper.createObjectNode();
            command.put("command", "deleteType");
            command.put("id", id);
            JsonNode abonomentResult = abonomentPluginRegister.execute(command);
            result.put("result", abonomentResult.get("result").booleanValue());
            if(abonomentResult.has("error")){
                result.put("error", abonomentResult.get("error").textValue());
            }
        } else {
            result.put("error", "abonomentPluginNotStarted");
        }

        return result;
    }

    @Override
    public JsonNode createAbonoment(String platenumber, Long parkingId, Long typeId, String dateStart, Boolean checked) throws Exception {

        ObjectNode result = objectMapper.createObjectNode();
        result.put("result", false);

        PluginRegister abonomentPluginRegister = pluginService.getPluginRegister(abonomentPlugin);
        if(abonomentPluginRegister != null){
            ObjectNode command = objectMapper.createObjectNode();
            command.put("command", "createAbonoment");
            command.put("platenumber", platenumber);
            command.put("parkingId", parkingId);
            command.put("typeId", typeId);
            command.put("dateStart", dateStart);
            command.put("checked", checked);
            JsonNode abonomentResult = abonomentPluginRegister.execute(command);
            result.put("result", abonomentResult.get("result").booleanValue());
            if(abonomentResult.has("error")){
                result.put("error", abonomentResult.get("error").textValue());
            }
        } else {
            result.put("error", "abonomentPluginNotStarted");
        }
        return result;
    }

    @Override
    public JsonNode deleteAbonoment(Long id) throws Exception {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("result", false);

        PluginRegister abonomentPluginRegister = pluginService.getPluginRegister(abonomentPlugin);
        if(abonomentPluginRegister != null){
            ObjectNode command = objectMapper.createObjectNode();
            command.put("command", "deleteAbonoment");
            command.put("id", id);
            JsonNode abonomentResult = abonomentPluginRegister.execute(command);
            result.put("result", abonomentResult.get("result").booleanValue());
            if(abonomentResult.has("error")){
                result.put("error", abonomentResult.get("error").textValue());
            }
        } else {
            result.put("error", "abonomentPluginNotStarted");
        }

        return result;
    }

    @Override
    public JsonNode getAbonomentsDetails(String plateNumber, CarState carState, SimpleDateFormat format) throws Exception {
        PluginRegister abonomentPluginRegister = pluginService.getPluginRegister(StaticValues.abonomentPlugin);
        if (abonomentPluginRegister != null) {
            ObjectNode node = this.objectMapper.createObjectNode();
            node.put("command", "getSatisfiedAbonomentDetails");
            node.put("plateNumber", plateNumber);
            node.put("parkingId", carState.getParking().getId());
            node.put("carInDate", format.format(carState.getInTimestamp()));

            JsonNode result = abonomentPluginRegister.execute(node);
            if(result.has("abonementsDetails")){
                return result.get("abonementsDetails");
            }
        }
        return null;
    }

    @Override
    public JsonNode getAbonomentsDetails(String plateNumber,Long parkingId, Date date, SimpleDateFormat format) throws Exception {
        PluginRegister abonomentPluginRegister = pluginService.getPluginRegister(StaticValues.abonomentPlugin);
        if (abonomentPluginRegister != null) {
            ObjectNode node = this.objectMapper.createObjectNode();
            node.put("command", "getSatisfiedAbonomentDetails");
            node.put("plateNumber", plateNumber);
            node.put("parkingId", parkingId);
            node.put("carInDate", format.format(date));
            JsonNode result = abonomentPluginRegister.execute(node);
            if(result.has("abonementsDetails")){
                return result.get("abonementsDetails");
            }
        }
        return null;
    }

    @Override
    public List<Period> calculatePaymentPeriods(JsonNode abonementJson, Date inDate, Date outDate) throws JsonProcessingException, ParseException {

        ArrayNode abonements = (ArrayNode) abonementJson;

        final String dateFormat = "dd.MM.yyyy HH:mm";
        SimpleDateFormat abonementFormat = new SimpleDateFormat(dateFormat);

        Iterator<JsonNode> iterator = abonements.iterator();
        List<Period> periods = new ArrayList<>();
        JsonNode prevAbonoment = null;
        Period p = null;

        while (iterator.hasNext()) {
            JsonNode abonoment = iterator.next();
            Date start = abonementFormat.parse(abonoment.get("begin").textValue());
            Date end = abonementFormat.parse(abonoment.get("end").textValue());
            String type = abonoment.has("type") && abonoment.get("type") != null ? abonoment.get("type").textValue() : null;
            String custom_numbers = abonoment.has("custom_numbers") && abonoment.get("custom_numbers") != null ? abonoment.get("custom_numbers").textValue() : null;

            if(prevAbonoment == null){
                if(inDate.before(start)){
                    p = new Period();
                    p.setStart(inDate);
                    p.setEnd(start);
                }
            } else {
                if(abonementFormat.parse(prevAbonoment.get("end").textValue()).getTime() - start.getTime() > 0){
                    if(p == null){
                        p = new Period();
                        p.setStart(inDate);
                        p.setEnd(start);
                    } else {
                        if(p.getEnd().equals(abonementFormat.parse(prevAbonoment.get("end").textValue()))){
                            p.setEnd(start);
                        } else {
                            periods.add(p);
                            p = new Period();
                            p.setStart(abonementFormat.parse(prevAbonoment.get("end").textValue()));
                            p.setEnd(start);
                        }
                    }
                }
            }

            if("CUSTOM".equals(type)){
                Calendar startCalendar = Calendar.getInstance();
                startCalendar.setTime(start.before(inDate) ? inDate : start);

                if(custom_numbers != null){
                    JsonNode custom_numbersJson = objectMapper.readTree(custom_numbers);

                    while (startCalendar.getTime().before(outDate.before(end) ? outDate : end)){
                        LocalDate localDate = LocalDate.of(startCalendar.get(Calendar.YEAR), startCalendar.get(Calendar.MONTH) + 1, startCalendar.get(Calendar.DAY_OF_MONTH));
                        int day = localDate.getDayOfWeek().getValue() - 1;
                        int hour = startCalendar.get(Calendar.HOUR_OF_DAY);

                        Boolean hasAccess = true;
                        if (custom_numbersJson.has(day + "")) {
                            TreeSet<Integer> sortedHours = new TreeSet<>();
                            for (final JsonNode h : custom_numbersJson.get("" + day)) {
                                sortedHours.add(h.intValue());
                            }
                            if (!sortedHours.contains(hour)) {
                                hasAccess = false;
                            }
                        } else {
                            hasAccess = false;
                        }

                        int minute = startCalendar.get(Calendar.MINUTE);
                        if(hasAccess){
                            startCalendar.add(Calendar.HOUR_OF_DAY, 1);
                            if(minute > 0){
                                startCalendar.set(Calendar.MINUTE, 0);
                                startCalendar.set(Calendar.SECOND, 0);
                                startCalendar.set(Calendar.MILLISECOND, 0);
                            }
                            if(startCalendar.getTime().after(outDate)){
                                startCalendar.setTime(outDate);
                            }
                        } else {
                            Date periodStart = startCalendar.getTime();
                            startCalendar.add(Calendar.HOUR_OF_DAY, 1);
                            if(minute > 0){
                                startCalendar.set(Calendar.MINUTE, 0);
                                startCalendar.set(Calendar.SECOND, 0);
                                startCalendar.set(Calendar.MILLISECOND, 0);
                            }
                            if(startCalendar.getTime().after(outDate)){
                                startCalendar.setTime(outDate);
                            }
                            Date periodEnd = startCalendar.getTime();

                            if(p == null){
                                p = new Period();
                                p.setStart(periodStart);
                                p.setEnd(periodEnd);
                            } else {
                                if(p.getEnd().equals(periodStart)){
                                    p.setEnd(periodEnd);
                                } else {
                                    periods.add(p);
                                    p = new Period();
                                    p.setStart(periodStart);
                                    p.setEnd(periodEnd);
                                }
                            }
                        }
                    }
                }
            }

            if(!iterator.hasNext()){
                if(outDate.after(end)){
                    Period period = new Period();
                    period.setStart(end);
                    period.setEnd(outDate);
                    periods.add(period);
                }
            } else {
                prevAbonoment = abonoment;
            }
        }

        if(p != null){
            periods.add(p);
        }

        return periods;
    }
}

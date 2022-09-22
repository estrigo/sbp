package kz.spt.app.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.app.model.dto.Period;
import kz.spt.app.service.WhitelistRootService;
import kz.spt.lib.extension.PluginRegister;
import kz.spt.lib.service.PluginService;
import kz.spt.lib.utils.StaticValues;
import lombok.extern.java.Log;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;

@Log
@Service
@Transactional
public class WhitelistRootServiceImpl implements WhitelistRootService {

    private final PluginService pluginService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WhitelistRootServiceImpl(PluginService pluginService){
        this.pluginService = pluginService;
    }

    public JsonNode getWhiteLists(Long parkingId, String car_number, Date event_time, SimpleDateFormat format, Map<String, Object> properties) throws Exception {
        ObjectNode node = objectMapper.createObjectNode();
        JsonNode whitelistCheckResult = null;
        node.put("parkingId", parkingId);
        node.put("car_number", car_number);
        node.put("event_time", format.format(event_time));

        PluginRegister whitelistPluginRegister = pluginService.getPluginRegister(StaticValues.whitelistPlugin);
        if (whitelistPluginRegister != null) {
            JsonNode result = whitelistPluginRegister.execute(node);
            whitelistCheckResult = result.get("whitelistCheckResult");
        }
        return whitelistCheckResult;
    }

    @Override
    public List<Period> calculatePaymentPeriods(JsonNode whitelistJson, Date inDate, Date outDate) throws JsonProcessingException, ParseException {

        ArrayNode whitelistArray = (ArrayNode) whitelistJson;

        final String dateFormat = "dd.MM.yyyy HH:mm";
        SimpleDateFormat whitelistFormat = new SimpleDateFormat(dateFormat);

        Iterator<JsonNode> iterator = whitelistArray.iterator();
        List<Period> periods = new ArrayList<>();
        while (iterator.hasNext()) {
            JsonNode whitelist = iterator.next();
            String type = whitelist.has("type") && whitelist.get("type") != null ? whitelist.get("type").textValue() : null;

            if("UNLIMITED".equals(type)){
                return new ArrayList<>();
            } else if("CUSTOM".equals(type)){
                String customJson = whitelist.has("customJson") && whitelist.get("customJson") != null ? whitelist.get("customJson").toString() : null;

                Calendar startCalendar = Calendar.getInstance();
                startCalendar.setTime(inDate);

                if(customJson != null){
                    JsonNode custom_numbersJson = objectMapper.readTree(customJson);

                    Period p = null;

                    while (startCalendar.getTime().before(outDate)){
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
                            Date start = startCalendar.getTime();
                            startCalendar.add(Calendar.HOUR_OF_DAY, 1);
                            if(minute > 0){
                                startCalendar.set(Calendar.MINUTE, 0);
                                startCalendar.set(Calendar.SECOND, 0);
                                startCalendar.set(Calendar.MILLISECOND, 0);
                            }
                            if(startCalendar.getTime().after(outDate)){
                                startCalendar.setTime(outDate);
                            }
                            Date end = startCalendar.getTime();

                            if(p == null){
                                p = new Period();
                                p.setStart(start);
                                p.setEnd(end);
                            } else {
                                if(p.getEnd().equals(start)){
                                    p.setEnd(end);
                                } else {
                                    periods.add(p);
                                    p = new Period();
                                    p.setStart(start);
                                    p.setEnd(end);
                                }
                            }
                        }
                    }

                    if(p != null){
                        periods.add(p);
                    }
                }
            } else {
                Date start = whitelist.has("accessStart") && whitelist.get("accessStart") != null? whitelistFormat.parse(whitelist.get("accessStart").textValue()) : inDate;
                Date end = whitelist.has("accessEnd") && whitelist.get("accessEnd") != null? whitelistFormat.parse(whitelist.get("accessEnd").textValue()) : null;

                if(inDate.before(start)){
                    Period period = new Period();
                    period.setStart(inDate);
                    period.setEnd(start);
                    periods.add(period);
                }
                if(outDate.after(end)){
                    Period period = new Period();
                    period.setStart(end);
                    period.setEnd(outDate);
                    periods.add(period);
                }
            }
        }

        return periods;
    }

    @Override
    public JsonNode getValidWhiteListsInPeriod(Long parkingId, String platenumber, Date inDate, Date outDate, SimpleDateFormat format) throws Exception {
        ObjectNode node = objectMapper.createObjectNode();
        JsonNode validWhiteListsInPeriodResult = null;
        node.put("parkingId", parkingId);
        node.put("car_number", platenumber);
        node.put("inDate", format.format(inDate));
        node.put("outDate", format.format(outDate));
        node.put("command", "getValidWhiteListsInPeriod");

        PluginRegister whitelistPluginRegister = pluginService.getPluginRegister(StaticValues.whitelistPlugin);
        if (whitelistPluginRegister != null) {
            JsonNode result = whitelistPluginRegister.execute(node);
            validWhiteListsInPeriodResult = result.get("validWhiteListsInPeriodResult");
        }
        return validWhiteListsInPeriodResult;
    }
}

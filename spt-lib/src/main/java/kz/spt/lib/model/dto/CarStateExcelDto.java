package kz.spt.lib.model.dto;


import kz.spt.lib.model.CarState;
import lombok.Data;
import org.springframework.context.i18n.LocaleContextHolder;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Data
public class CarStateExcelDto {

    private static String dateFormat = "dd.MM.yyyy HH:mm:ss";

    public String carNumber;
    public String inTimestampString;
    public String outTimestampString;
    public String inGate;
    public String outGate;
    public String status;
    public String duration;

    public static CarStateExcelDto fromCarState(CarState carState) {

        SimpleDateFormat format = new SimpleDateFormat(dateFormat);

        CarStateExcelDto dto = new CarStateExcelDto();
        dto.carNumber = carState.getCarNumber();
        dto.inTimestampString = carState.getInTimestamp() == null ? "" : format.format(carState.getInTimestamp());
        dto.outTimestampString = carState.getOutTimestamp() == null ? "" : format.format(carState.getOutTimestamp());
        if (carState.getInGate() != null) {
            dto.inGate = carState.getInGate().getName();
        }
        if (carState.getOutGate() != null) {
            dto.outGate = carState.getOutGate().getName();
        }
        StringBuilder durationBuilder = new StringBuilder("");
        if (carState.getInTimestamp() != null) {
            Locale locale = LocaleContextHolder.getLocale();
            String language = locale.getLanguage();

            long time_difference = (carState.getOutTimestamp() == null ? (new Date()).getTime() : carState.getOutTimestamp().getTime()) - carState.getInTimestamp().getTime();
            long days_difference = TimeUnit.MILLISECONDS.toDays(time_difference) % 365;
            if (days_difference > 0) {
                durationBuilder.append(days_difference + (language.equals("ru") ? "д " : "d "));
            }

            long hours_difference = TimeUnit.MILLISECONDS.toHours(time_difference) % 24;
            if (hours_difference > 0 || durationBuilder.length() > 0) {
                durationBuilder.append(hours_difference + (language.equals("ru") ? "ч " : "h "));
            }

            long minutes_difference = TimeUnit.MILLISECONDS.toMinutes(time_difference) % 60;
            if (minutes_difference > 0 || durationBuilder.length() > 0) {
                durationBuilder.append(minutes_difference + (language.equals("ru") ? "м " : "m "));
            }

            long seconds_difference = TimeUnit.MILLISECONDS.toSeconds(time_difference) % 60;
            if (seconds_difference > 0 || durationBuilder.length() > 0) {
                durationBuilder.append(seconds_difference + (language.equals("ru") ? "с " : "s "));
            }
        }
        dto.duration = durationBuilder.toString();

//        ResourceBundle bundle = ResourceBundle.getBundle("messages", Locale.forLanguageTag("ru".equals(LocaleContextHolder.getLocale().toString()) ? "ru-RU" : "en"));
//        dto.status = bundle.getString("carState.active");
//        if (carState.getOutTimestamp() != null) {
//            dto.status = bundle.getString("carState.completed");
//        }
        return dto;
    }

    public static List<CarStateExcelDto> fromCarStates(List<CarState> carStates) {
        List<CarStateExcelDto> carStateDtos = new ArrayList<>(carStates.size());
        for (CarState carState : carStates) {
            carStateDtos.add(fromCarState(carState));
        }
        return carStateDtos;
    }
}

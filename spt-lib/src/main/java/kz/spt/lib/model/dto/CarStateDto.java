package kz.spt.lib.model.dto;

import kz.spt.lib.model.CarState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Data
public class CarStateDto {

    private static String dateFormat = "dd.MM.yyyy HH:mm:ss";

    public Long id;
    public String carNumber;
    public Date inTimestamp;
    public Date outTimestamp;
    public String inTimestampString;
    public String outTimestampString;
    public Boolean paid;
    public BigDecimal payment;
    public String duration;
    public String whitelistJson;
    public String paymentJson;
    public BigDecimal rateAmount;
    public String inGate;
    public String outGate;
    public String parking;
    public String css;
    public String abonomentJson;

    public Date getNullSafeInTimestamp() {
        return this.inTimestamp == null ? new Date() : this.inTimestamp;
    }

    public Date getNullSafeOutTimestamp() {
        return this.outTimestamp == null ? new Date() : this.outTimestamp;
    }

    public BigDecimal getNullSafePayment() {
        return this.payment == null ? BigDecimal.ZERO : this.payment;
    }

    public static CarStateDto fromCarState(CarState carState) {

        SimpleDateFormat format = new SimpleDateFormat(dateFormat);

        CarStateDto dto = new CarStateDto();
        dto.carNumber = carState.getCarNumber();
        dto.inTimestamp = carState.getInTimestamp();
        dto.outTimestamp = carState.getOutTimestamp();
        dto.inTimestampString = carState.getInTimestamp() == null ? "" : format.format(carState.getInTimestamp());
        dto.outTimestampString = carState.getOutTimestamp() == null ? "" : format.format(carState.getOutTimestamp());
        dto.id = carState.getId();
        dto.whitelistJson = carState.getWhitelistJson();
        dto.paymentJson = carState.getPaymentJson();
        dto.abonomentJson = carState.getAbonomentJson();
        dto.rateAmount = carState.getRateAmount();
        dto.paid = carState.getPaid() != null && carState.getPaid();
        dto.payment = carState.getAmount() == null ? BigDecimal.ZERO : carState.getAmount();

        if (carState.getInGate() != null) {
            dto.inGate = carState.getInGate().getName() + ", " + carState.getParking().getName();
        }
        if (carState.getOutGate() != null) {
            dto.outGate = carState.getOutGate().getName() + ", " + carState.getParking().getName();
        }

        StringBuilder durationBuilder = new StringBuilder("");
        if (carState.getInTimestamp() != null) {
            long time_difference = (carState.getOutTimestamp() == null ? (new Date()).getTime() : carState.getOutTimestamp().getTime()) - carState.getInTimestamp().getTime();
            long days_difference = TimeUnit.MILLISECONDS.toDays(time_difference) % 365;
            if (days_difference > 0) {
                durationBuilder.append(days_difference + "д. ");
            }

            long hours_difference = TimeUnit.MILLISECONDS.toHours(time_difference) % 24;
            if (hours_difference > 0 || durationBuilder.length() > 0) {
                durationBuilder.append(hours_difference + "ч. ");
            }

            long minutes_difference = TimeUnit.MILLISECONDS.toMinutes(time_difference) % 60;
            if (minutes_difference > 0 || durationBuilder.length() > 0) {
                durationBuilder.append(minutes_difference + "мин. ");
            }

            long seconds_difference = TimeUnit.MILLISECONDS.toSeconds(time_difference) % 60;
            if (seconds_difference > 0 || durationBuilder.length() > 0) {
                durationBuilder.append(seconds_difference + "сек. ");
            }

            if (carState.getOutTimestamp() == null &&
                    (days_difference > 0 || (days_difference <= 0 && hours_difference >= 16))) {
                dto.css = "table-danger";
            }
        }
        dto.duration = durationBuilder.toString();
        return dto;
    }

    public static List<CarStateDto> fromCarStates(List<CarState> carStates) {
        List<CarStateDto> carStateDtos = new ArrayList<>();
        for (CarState carState : carStates) {
            carStateDtos.add(fromCarState(carState));
        }
        return carStateDtos;
    }
}

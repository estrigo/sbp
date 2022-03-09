package kz.spt.lib.model.dto;

import kz.spt.lib.model.CarState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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
    public String inImageUrl;
    public String outImageUrl;

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
        dto.inImageUrl  = carState.getInPhotoUrl();
        dto.outImageUrl  = carState.getOutPhotoUrl();

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

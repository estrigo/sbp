package kz.spt.lib.model.dto;

import kz.spt.lib.utils.Utils;
import lombok.ToString;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.Date;

@ToString
public class CarEventDto {

    @NotNull
    @Pattern(regexp = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$")
    public String ip_address;

    public String event_time;
    public Date event_date_time;

    @NotNull
    @Size(min = 3, max = 16)
    public String car_number;

    @NotNull
    public String car_picture;

    @NotNull
    public String lp_picture;

    @NotNull
    public String lp_rect;
    public Boolean manualEnter = false; //Ручной запуск авто через набор номера
    public Boolean manualOpen = false; //Ручное открытие шлагбаума

    public String lp_region;
    public String lp_country;
    public String vecihleType;
    public String car_model;

    public Long cameraId = null;

    public String getCarNumberWithRegion() {
        return (this.lp_region != null ? Utils.convertRegion(this.lp_region) + " " : "") + this.car_number;
    }
}

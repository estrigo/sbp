package kz.spt.lib.model.dto.adminPlace;

import org.springframework.context.ApplicationEvent;

import java.util.Objects;

public class WhiteListEvent extends ApplicationEvent {

    private Long car_id;
    private String platenumber;
    private Long group_id;
    private String model;

    public WhiteListEvent(Object source, Long car_id, String platenumber, Long group_id, String model) {
        super(source);
        this.car_id = car_id;
        this.platenumber = platenumber;
        this.group_id = group_id;
        this.model = model;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WhiteListEvent that = (WhiteListEvent) o;
        return Objects.equals(car_id, that.car_id) && Objects.equals(platenumber, that.platenumber) && Objects.equals(group_id, that.group_id) && Objects.equals(model, that.model);
    }

    @Override
    public int hashCode() {
        return Objects.hash(car_id, platenumber, group_id, model);
    }

    public Long getCar_id() {
        return car_id;
    }

    public void setCar_id(Long car_id) {
        this.car_id = car_id;
    }

    public String getPlatenumber() {
        return platenumber;
    }

    public void setPlatenumber(String platenumber) {
        this.platenumber = platenumber;
    }

    public Long getGroup_id() {
        return group_id;
    }

    public void setGroup_id(Long group_id) {
        this.group_id = group_id;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}

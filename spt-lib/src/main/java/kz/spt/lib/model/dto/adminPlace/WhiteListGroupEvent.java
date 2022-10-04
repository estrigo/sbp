package kz.spt.lib.model.dto.adminPlace;


import org.springframework.context.ApplicationEvent;

import java.util.Objects;

public class WhiteListGroupEvent extends ApplicationEvent {
    private String name;
    private Long id;
    private Long parking_id;

    public WhiteListGroupEvent(Object source, String name, Long id, Long parking_id) {
        super(source);
        this.name = name;
        this.id = id;
        this.parking_id = parking_id;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WhiteListGroupEvent that = (WhiteListGroupEvent) o;
        return Objects.equals(name, that.name) && Objects.equals(id, that.id) && Objects.equals(parking_id, that.parking_id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, id, parking_id);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getParking_id() {
        return parking_id;
    }

    public void setParking_id(Long parking_id) {
        this.parking_id = parking_id;
    }
}

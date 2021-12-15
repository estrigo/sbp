package kz.spt.lib.bootstrap.datatable;

import kz.spt.lib.model.dto.EventsDto;
import kz.spt.lib.service.CarsService;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public final class EventComparator {

    @EqualsAndHashCode
    @AllArgsConstructor
    @Getter
    static class Key {
        String name;
        Direction dir;
    }
    @Autowired
    public CarsService carsService;
    static Map<EventComparator.Key, Comparator<EventsDto>> map = new HashMap<>();

    static {
        map.put(new EventComparator.Key("created", Direction.asc), Comparator.comparing(EventsDto::getCreated));
        map.put(new EventComparator.Key("created", Direction.desc), Comparator.comparing(EventsDto::getCreated).reversed());

        map.put(new EventComparator.Key("plateNumber", Direction.asc), Comparator.comparing((EventsDto::getPlateNumber)));
        map.put(new EventComparator.Key("plateNumber", Direction.desc), Comparator.comparing(EventsDto::getPlateNumber).reversed());

        map.put(new EventComparator.Key("description", Direction.asc), Comparator.comparing(EventsDto::getDescription));
        map.put(new EventComparator.Key("description", Direction.desc), Comparator.comparing(EventsDto::getDescription).reversed());
    }

    public static Comparator<EventsDto> getComparator(String name, Direction dir) {
        return map.get(new EventComparator.Key(name, dir));
    }

    private EventComparator() {
    }
}


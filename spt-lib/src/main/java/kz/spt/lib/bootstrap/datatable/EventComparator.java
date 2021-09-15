package kz.spt.lib.bootstrap.datatable;

import kz.spt.lib.model.EventLog;
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
    static Map<EventComparator.Key, Comparator<EventLog>> map = new HashMap<>();

    static {
        map.put(new EventComparator.Key("created", Direction.asc), Comparator.comparing(EventLog::getCreated));
        map.put(new EventComparator.Key("created", Direction.desc), Comparator.comparing(EventLog::getCreated).reversed());

        map.put(new EventComparator.Key("plateNumber", Direction.asc), Comparator.comparing((EventLog::getNullSafePlateNumber)));
        map.put(new EventComparator.Key("plateNumber", Direction.desc), Comparator.comparing(EventLog::getNullSafePlateNumber).reversed());

        map.put(new EventComparator.Key("description", Direction.asc), Comparator.comparing(EventLog::getNullSafeDescription));
        map.put(new EventComparator.Key("description", Direction.desc), Comparator.comparing(EventLog::getNullSafeDescription).reversed());
    }

    public static Comparator<EventLog> getComparator(String name, Direction dir) {
        return map.get(new EventComparator.Key(name, dir));
    }

    private EventComparator() {
    }
}


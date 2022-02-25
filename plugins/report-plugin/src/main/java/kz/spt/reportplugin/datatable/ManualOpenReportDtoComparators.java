package kz.spt.reportplugin.datatable;

import kz.spt.lib.bootstrap.datatable.Direction;
import kz.spt.lib.model.dto.EventsDto;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public final class ManualOpenReportDtoComparators {
    static Map<ManualOpenReportDtoComparators.Key, Comparator<EventsDto>> map = new HashMap<>();

    static {
        map.put(new ManualOpenReportDtoComparators.Key("id", Direction.asc), Comparator.comparing(EventsDto::getId));
        map.put(new ManualOpenReportDtoComparators.Key("id", Direction.desc), Comparator.comparing(EventsDto::getId).reversed());

        map.put(new ManualOpenReportDtoComparators.Key("created", Direction.asc), Comparator.comparing(EventsDto::getCreated));
        map.put(new ManualOpenReportDtoComparators.Key("created", Direction.desc), Comparator.comparing(EventsDto::getCreated).reversed());

        map.put(new ManualOpenReportDtoComparators.Key("plateNumber", Direction.asc), Comparator.comparing(EventsDto::getPlateNumber));
        map.put(new ManualOpenReportDtoComparators.Key("plateNumber", Direction.desc), Comparator.comparing(EventsDto::getPlateNumber).reversed());
    }

    public ManualOpenReportDtoComparators() {

    }

    public static Comparator<EventsDto> getComparator(String name, Direction dir) {
        return map.get(new ManualOpenReportDtoComparators.Key(name, dir));
    }

    @EqualsAndHashCode
    @AllArgsConstructor
    @Getter
    static class Key {
        String name;
        Direction dir;
    }
}

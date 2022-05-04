package kz.spt.reportplugin.datatable;

import kz.spt.lib.bootstrap.datatable.Direction;
import kz.spt.reportplugin.dto.SumReportDto;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public final class SumReportDtoComparators {

    @EqualsAndHashCode
    @AllArgsConstructor
    @Getter
    static class Key {
        String name;
        Direction dir;
    }

    static Map<Key, Comparator<SumReportDto>> map = new HashMap<>();

    static {
       /* map.put(new Key("carStateId", Direction.asc), Comparator.comparing(JournalReportDto::getCarStateId));
        map.put(new Key("carStateId", Direction.desc), Comparator.comparing(JournalReportDto::getCarStateId).reversed());

        map.put(new Key("inTimestamp", Direction.asc), Comparator.comparing(JournalReportDto::getInTimestamp));
        map.put(new Key("inTimestamp", Direction.desc), Comparator.comparing(JournalReportDto::getInTimestamp).reversed());

        map.put(new Key("outTimestamp", Direction.asc), Comparator.comparing(JournalReportDto::getOutTimestamp));
        map.put(new Key("outTimestamp", Direction.desc), Comparator.comparing(JournalReportDto::getOutTimestamp).reversed());

        map.put(new Key("sum", Direction.asc), Comparator.comparing(JournalReportDto::getSum));
        map.put(new Key("sum", Direction.desc), Comparator.comparing(JournalReportDto::getSum).reversed());

        map.put(new Key("carNumber", Direction.asc), Comparator.comparing(JournalReportDto::getCarNumber));
        map.put(new Key("carNumber", Direction.desc), Comparator.comparing(JournalReportDto::getCarNumber).reversed());

        map.put(new Key("provider", Direction.asc), Comparator.comparing(JournalReportDto::getProvider));
        map.put(new Key("provider", Direction.desc), Comparator.comparing(JournalReportDto::getProvider).reversed());

        map.put(new Key("parkingTypeCode", Direction.asc), Comparator.comparing(JournalReportDto::getParkingTypeCode));
        map.put(new Key("parkingTypeCode", Direction.desc), Comparator.comparing(JournalReportDto::getParkingTypeCode).reversed());
*/    }

    public static Comparator<SumReportDto> getComparator(String name, Direction dir) {
        return map.get(new Key(name, dir));
    }

    private SumReportDtoComparators() {
    }
}

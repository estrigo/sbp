package kz.spt.abonomentplugin.bootstrap.datatable;

import kz.spt.abonomentplugin.dto.AbonomentDTO;
import kz.spt.abonomentplugin.dto.AbonomentTypeDTO;
import kz.spt.lib.bootstrap.datatable.Direction;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public final class AbonomentDtoComparators {

    @EqualsAndHashCode
    @AllArgsConstructor
    @Getter
    static class Key {
        String name;
        Direction dir;
    }

    static Map<Key, Comparator<AbonomentDTO>> map = new HashMap<>();

    static {
        map.put(new Key("platenumber", Direction.asc), Comparator.comparing(AbonomentDTO::getPlatenumber));
        map.put(new Key("platenumber", Direction.desc), Comparator.comparing(AbonomentDTO::getPlatenumber).reversed());

        map.put(new Key("begin", Direction.asc), Comparator.comparing(AbonomentDTO::getBegin));
        map.put(new Key("begin", Direction.desc), Comparator.comparing(AbonomentDTO::getBegin).reversed());

        map.put(new Key("end", Direction.asc), Comparator.comparing(AbonomentDTO::getEnd));
        map.put(new Key("end", Direction.desc), Comparator.comparing(AbonomentDTO::getEnd).reversed());

        map.put(new Key("months", Direction.asc), Comparator.comparing(AbonomentDTO::getMonths));
        map.put(new Key("months", Direction.desc), Comparator.comparing(AbonomentDTO::getMonths).reversed());

        map.put(new Key("price", Direction.asc), Comparator.comparing(AbonomentDTO::getPrice));
        map.put(new Key("price", Direction.desc), Comparator.comparing(AbonomentDTO::getPrice).reversed());

        map.put(new Key("parking", Direction.asc), Comparator.comparing(AbonomentDTO::getParking));
        map.put(new Key("parking", Direction.desc), Comparator.comparing(AbonomentDTO::getParking).reversed());
    }

    public static Comparator<AbonomentDTO> getComparator(String name, Direction dir) {
        return map.get(new Key(name, dir));
    }

    private AbonomentDtoComparators() {
    }
}

package kz.spt.abonomentplugin.bootstrap.datatable;

import kz.spt.abonomentplugin.dto.AbonomentTypeDTO;
import kz.spt.lib.bootstrap.datatable.Direction;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public final class AbonomentTypeDtoComparators {

    @EqualsAndHashCode
    @AllArgsConstructor
    @Getter
    static class Key {
        String name;
        Direction dir;
    }

    static Map<Key, Comparator<AbonomentTypeDTO>> map = new HashMap<>();

    static {
        map.put(new Key("period", Direction.asc), Comparator.comparing(AbonomentTypeDTO::getPeriod));
        map.put(new Key("period", Direction.desc), Comparator.comparing(AbonomentTypeDTO::getPrice).reversed());
    }

    public static Comparator<AbonomentTypeDTO> getComparator(String name, Direction dir) {
        return map.get(new Key(name, dir));
    }

    private AbonomentTypeDtoComparators() {
    }
}

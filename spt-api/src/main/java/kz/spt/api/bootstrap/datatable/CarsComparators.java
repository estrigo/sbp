package kz.spt.api.bootstrap.datatable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import kz.spt.api.model.Cars;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

public final class CarsComparators {

    @EqualsAndHashCode
    @AllArgsConstructor
    @Getter
    static class Key {
        String name;
        Direction dir;
    }

    static Map<Key, Comparator<Cars>> map = new HashMap<>();

    static {
        map.put(new Key("platenumber", Direction.asc), Comparator.comparing(Cars::getPlatenumber));
        map.put(new Key("platenumber", Direction.desc), Comparator.comparing(Cars::getPlatenumber).reversed());

        map.put(new Key("brand", Direction.asc), Comparator.comparing(Cars::getNullSafeBrand));
        map.put(new Key("brand", Direction.desc), Comparator.comparing(Cars::getNullSafeBrand).reversed());

        map.put(new Key("color", Direction.asc), Comparator.comparing(Cars::getNullSafeColor));
        map.put(new Key("color", Direction.desc), Comparator.comparing(Cars::getNullSafeColor).reversed());
    }

    public static Comparator<Cars> getComparator(String name, Direction dir) {
        return map.get(new Key(name, dir));
    }

    private CarsComparators() {
    }
}

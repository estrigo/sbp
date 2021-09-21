package kz.spt.lib.bootstrap.datatable;

import kz.spt.lib.model.CarState;
import kz.spt.lib.model.dto.CarStateDto;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public final class CarStateDtoComparators {

    @EqualsAndHashCode
    @AllArgsConstructor
    @Getter
    static class Key {
        String name;
        Direction dir;
    }

    static Map<Key, Comparator<CarStateDto>> map = new HashMap<>();

    static {
        map.put(new Key("id", Direction.asc), Comparator.comparing(CarStateDto::getId));
        map.put(new Key("id", Direction.desc), Comparator.comparing(CarStateDto::getId).reversed());

        map.put(new Key("carNumber", Direction.asc), Comparator.comparing(CarStateDto::getCarNumber));
        map.put(new Key("carNumber", Direction.desc), Comparator.comparing(CarStateDto::getCarNumber).reversed());

        map.put(new Key("inTimestampString", Direction.asc), Comparator.comparing(CarStateDto::getNullSafeInTimestamp));
        map.put(new Key("inTimestampString", Direction.desc), Comparator.comparing(CarStateDto::getNullSafeInTimestamp).reversed());

        map.put(new Key("outTimestampString", Direction.asc), Comparator.comparing(CarStateDto::getNullSafeOutTimestamp));
        map.put(new Key("outTimestampString", Direction.desc), Comparator.comparing(CarStateDto::getNullSafeOutTimestamp).reversed());

        map.put(new Key("payment", Direction.asc), Comparator.comparing(CarStateDto::getNullSafePayment));
        map.put(new Key("payment", Direction.desc), Comparator.comparing(CarStateDto::getNullSafePayment).reversed());

        map.put(new Key("paid", Direction.asc), Comparator.comparing(CarStateDto::getPaid));
        map.put(new Key("paid", Direction.desc), Comparator.comparing(CarStateDto::getPaid).reversed());

        map.put(new Key("duration", Direction.asc), Comparator.comparing(CarStateDto::getDuration));
        map.put(new Key("duration", Direction.desc), Comparator.comparing(CarStateDto::getDuration).reversed());
    }

    public static Comparator<CarStateDto> getComparator(String name, Direction dir) {
        return map.get(new Key(name, dir));
    }

    private CarStateDtoComparators() {
    }
}

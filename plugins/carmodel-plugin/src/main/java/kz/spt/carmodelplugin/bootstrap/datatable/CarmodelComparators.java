package kz.spt.carmodelplugin.bootstrap.datatable;

import kz.spt.carmodelplugin.viewmodel.CarmodelDto;
import kz.spt.lib.bootstrap.datatable.Direction;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class CarmodelComparators {

    @EqualsAndHashCode
    @AllArgsConstructor
    @Getter
    static class Key {
        String name;
        Direction dir;
    }

    static Map<Key, Comparator<CarmodelDto>> map = new HashMap<>();

    public static Comparator<CarmodelDto> getComparator(String name, Direction dir) {
        return map.get(new Key(name, dir));
    }
    static {
        map.put(new Key("platenumber", Direction.asc), Comparator.comparing(CarmodelDto::getPlateNumber));
        map.put(new Key("platenumber", Direction.desc), Comparator.comparing(CarmodelDto::getPlateNumber).reversed());

        map.put(new Key("entryDate", Direction.asc), Comparator.comparing(CarmodelDto::getEntryDate));
        map.put(new Key("entryDate", Direction.desc), Comparator.comparing(CarmodelDto::getEntryDate).reversed());

        map.put(new Key("dimension", Direction.asc), Comparator.comparing(CarmodelDto::getDimension));
        map.put(new Key("dimension", Direction.desc), Comparator.comparing(CarmodelDto::getDimension).reversed());
    }

    private CarmodelComparators() {
    }

}

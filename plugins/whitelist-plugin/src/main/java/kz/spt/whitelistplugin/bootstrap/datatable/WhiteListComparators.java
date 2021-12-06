package kz.spt.whitelistplugin.bootstrap.datatable;

import kz.spt.lib.bootstrap.datatable.Direction;
import kz.spt.whitelistplugin.viewmodel.WhiteListDto;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public final class WhiteListComparators {

    @EqualsAndHashCode
    @AllArgsConstructor
    @Getter
    static class Key {
        String name;
        Direction dir;
    }

    static Map<Key, Comparator<WhiteListDto>> map = new HashMap<>();

    static {
        map.put(new Key("plateNumber", Direction.asc), Comparator.comparing(WhiteListDto::getPlateNumber));
        map.put(new Key("plateNumber", Direction.desc), Comparator.comparing(WhiteListDto::getPlateNumber).reversed());

        map.put(new Key("parkingName", Direction.asc), Comparator.comparing(WhiteListDto::getParkingName));
        map.put(new Key("parkingName", Direction.desc), Comparator.comparing(WhiteListDto::getParkingName).reversed());

        map.put(new Key("groupName", Direction.asc), Comparator.comparing(WhiteListDto::getGroupName));
        map.put(new Key("groupName", Direction.desc), Comparator.comparing(WhiteListDto::getGroupName).reversed());

        map.put(new Key("conditionDetail", Direction.asc), Comparator.comparing(WhiteListDto::getConditionDetail));
        map.put(new Key("conditionDetail", Direction.desc), Comparator.comparing(WhiteListDto::getConditionDetail).reversed());

        map.put(new Key("id", Direction.asc), Comparator.comparing(WhiteListDto::getId));
        map.put(new Key("id", Direction.desc), Comparator.comparing(WhiteListDto::getId).reversed());
    }

    public static Comparator<WhiteListDto> getComparator(String name, Direction dir) {
        return map.get(new Key(name, dir));
    }

    private WhiteListComparators(){

    }
}

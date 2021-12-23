package kz.spt.whitelistplugin.bootstrap.datatable;

import kz.spt.lib.bootstrap.datatable.Direction;
import kz.spt.whitelistplugin.viewmodel.WhiteListDto;
import kz.spt.whitelistplugin.viewmodel.WhiteListGroupDto;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class WhiteListGroupComparators {
    @EqualsAndHashCode
    @AllArgsConstructor
    @Getter
    static class Key {
        String name;
        Direction dir;
    }

    static Map<WhiteListGroupComparators.Key, Comparator<WhiteListGroupDto>> map = new HashMap<>();

    static {
        map.put(new WhiteListGroupComparators.Key("name", Direction.asc), Comparator.comparing(WhiteListGroupDto::getName));
        map.put(new WhiteListGroupComparators.Key("name", Direction.desc), Comparator.comparing(WhiteListGroupDto::getName).reversed());

        map.put(new WhiteListGroupComparators.Key("parkingName", Direction.asc), Comparator.comparing(WhiteListGroupDto::getParkingName));
        map.put(new WhiteListGroupComparators.Key("parkingName", Direction.desc), Comparator.comparing(WhiteListGroupDto::getParkingName).reversed());

        map.put(new WhiteListGroupComparators.Key("size", Direction.asc), Comparator.comparing(WhiteListGroupDto::getSize));
        map.put(new WhiteListGroupComparators.Key("size", Direction.desc), Comparator.comparing(WhiteListGroupDto::getSize).reversed());

        map.put(new WhiteListGroupComparators.Key("conditionDetail", Direction.asc), Comparator.comparing(WhiteListGroupDto::getConditionDetail));
        map.put(new WhiteListGroupComparators.Key("conditionDetail", Direction.desc), Comparator.comparing(WhiteListGroupDto::getConditionDetail).reversed());

        map.put(new WhiteListGroupComparators.Key("id", Direction.asc), Comparator.comparing(WhiteListGroupDto::getId));
        map.put(new WhiteListGroupComparators.Key("id", Direction.desc), Comparator.comparing(WhiteListGroupDto::getId).reversed());
    }

    public static Comparator<WhiteListGroupDto> getComparator(String name, Direction dir) {
        return map.get(new WhiteListGroupComparators.Key(name, dir));
    }

    private WhiteListGroupComparators(){

    }
}

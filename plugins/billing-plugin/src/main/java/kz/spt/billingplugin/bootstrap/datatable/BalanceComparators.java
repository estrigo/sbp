package kz.spt.billingplugin.bootstrap.datatable;

import kz.spt.billingplugin.model.Balance;
import kz.spt.lib.bootstrap.datatable.Direction;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public final class BalanceComparators {

    @EqualsAndHashCode
    @AllArgsConstructor
    @Getter
    static class Key {
        String name;
        Direction dir;
    }

    static Map<Key, Comparator<Balance>> map = new HashMap<>();

    static {
        map.put(new Key("plateNumber", Direction.asc), Comparator.comparing(Balance::getPlateNumber));
        map.put(new Key("plateNumber", Direction.desc), Comparator.comparing(Balance::getPlateNumber).reversed());

        map.put(new Key("balance", Direction.asc), Comparator.comparing(Balance::getBalance));
        map.put(new Key("balance", Direction.desc), Comparator.comparing(Balance::getBalance).reversed());

    }

    public static Comparator<Balance> getComparator(String name, Direction dir) {
        return map.get(new Key(name, dir));
    }

    private BalanceComparators() {

    }

}

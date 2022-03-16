package kz.spt.billingplugin.bootstrap.datatable;

import kz.spt.billingplugin.model.Transaction;
import kz.spt.lib.bootstrap.datatable.Direction;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public final class TransactionComparators {

    @EqualsAndHashCode
    @AllArgsConstructor
    @Getter
    static class Key {
        String name;
        Direction dir;
    }

    static Map<Key, Comparator<Transaction>> map = new HashMap<>();

    static {
        map.put(new Key("plateNumber", Direction.asc), Comparator.comparing(Transaction::getPlateNumber));
        map.put(new Key("plateNumber", Direction.desc), Comparator.comparing(Transaction::getPlateNumber).reversed());

        map.put(new Key("amount", Direction.asc), Comparator.comparing(Transaction::getAmount));
        map.put(new Key("amount", Direction.desc), Comparator.comparing(Transaction::getAmount).reversed());

        map.put(new Key("date", Direction.asc), Comparator.comparing(Transaction::getDate));
        map.put(new Key("date", Direction.desc), Comparator.comparing(Transaction::getDate).reversed());
    }

    public static Comparator<Transaction> getComparator(String name, Direction dir) {
        return map.get(new Key(name, dir));
    }

    private TransactionComparators() {

    }

}

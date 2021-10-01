package kz.spt.billingplugin.bootstrap.datatable;

import kz.spt.billingplugin.model.PaymentProvider;
import kz.spt.lib.bootstrap.datatable.Direction;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public final class PaymentProviderComparators {

    @EqualsAndHashCode
    @AllArgsConstructor
    @Getter
    static class Key {
        String name;
        Direction dir;
    }

    static Map<Key, Comparator<PaymentProvider>> map = new HashMap<>();

    static {
        map.put(new Key("id", Direction.asc), Comparator.comparing(PaymentProvider::getId));
        map.put(new Key("id", Direction.desc), Comparator.comparing(PaymentProvider::getId).reversed());

        map.put(new Key("provider", Direction.asc), Comparator.comparing(PaymentProvider::getProvider));
        map.put(new Key("provider", Direction.desc), Comparator.comparing(PaymentProvider::getProvider).reversed());

        map.put(new Key("name", Direction.asc), Comparator.comparing(PaymentProvider::getName));
        map.put(new Key("name", Direction.desc), Comparator.comparing(PaymentProvider::getName).reversed());

        map.put(new Key("enabled", Direction.asc), Comparator.comparing(PaymentProvider::getEnabled));
        map.put(new Key("enabled", Direction.desc), Comparator.comparing(PaymentProvider::getEnabled).reversed());
    }

    public static Comparator<PaymentProvider> getComparator(String name, Direction dir) {
        return map.get(new Key(name, dir));
    }

    private PaymentProviderComparators() {
    }
}

package kz.spt.billingplugin.bootstrap.datatable;

import kz.spt.billingplugin.dto.PaymentLogDTO;
import kz.spt.billingplugin.model.PaymentProvider;
import kz.spt.lib.bootstrap.datatable.Direction;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public final class PaymentDtoComparators {

    @EqualsAndHashCode
    @AllArgsConstructor
    @Getter
    static class Key {
        String name;
        Direction dir;
    }

    static Map<Key, Comparator<PaymentLogDTO>> map = new HashMap<>();

    static {
        map.put(new Key("id", Direction.asc), Comparator.comparing(PaymentLogDTO::getId));
        map.put(new Key("id", Direction.desc), Comparator.comparing(PaymentLogDTO::getId).reversed());

        map.put(new Key("created", Direction.asc), Comparator.comparing(PaymentLogDTO::getCreated));
        map.put(new Key("created", Direction.desc), Comparator.comparing(PaymentLogDTO::getCreated).reversed());

        map.put(new Key("inDate", Direction.asc), Comparator.comparing(PaymentLogDTO::getNullSafeInDate));
        map.put(new Key("inDate", Direction.desc), Comparator.comparing(PaymentLogDTO::getNullSafeInDate).reversed());

        map.put(new Key("outDate", Direction.asc), Comparator.comparing(PaymentLogDTO::getNullSafeOutDate));
        map.put(new Key("outDate", Direction.desc), Comparator.comparing(PaymentLogDTO::getNullSafeOutDate).reversed());

        map.put(new Key("rateDetails", Direction.asc), Comparator.comparing(PaymentLogDTO::getRateDetails));
        map.put(new Key("rateDetails", Direction.desc), Comparator.comparing(PaymentLogDTO::getRateDetails).reversed());

        map.put(new Key("price", Direction.asc), Comparator.comparing(PaymentLogDTO::getPrice));
        map.put(new Key("price", Direction.desc), Comparator.comparing(PaymentLogDTO::getPrice).reversed());

        map.put(new Key("parking", Direction.asc), Comparator.comparing(PaymentLogDTO::getParking));
        map.put(new Key("parking", Direction.desc), Comparator.comparing(PaymentLogDTO::getParking).reversed());

        map.put(new Key("carNumber", Direction.asc), Comparator.comparing(PaymentLogDTO::getCarNumber));
        map.put(new Key("carNumber", Direction.desc), Comparator.comparing(PaymentLogDTO::getCarNumber).reversed());

        map.put(new Key("provider", Direction.asc), Comparator.comparing(PaymentLogDTO::getProvider));
        map.put(new Key("provider", Direction.desc), Comparator.comparing(PaymentLogDTO::getProvider).reversed());

        map.put(new Key("transaction", Direction.asc), Comparator.comparing(PaymentLogDTO::getTransaction));
        map.put(new Key("transaction", Direction.desc), Comparator.comparing(PaymentLogDTO::getTransaction).reversed());
    }

    public static Comparator<PaymentLogDTO> getComparator(String name, Direction dir) {
        return map.get(new Key(name, dir));
    }

    private PaymentDtoComparators() {
    }
}

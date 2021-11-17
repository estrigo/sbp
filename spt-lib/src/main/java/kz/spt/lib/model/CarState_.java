package kz.spt.lib.model;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.math.BigDecimal;
import java.util.Date;

@StaticMetamodel(CarState.class)
public class CarState_ {

    public static volatile SingularAttribute<CarState, Long> id;

    public static volatile SingularAttribute<CarState, String> carNumber;

    public static volatile SingularAttribute<CarState, Date> inTimestamp;

    public static volatile SingularAttribute<CarState, Date> outTimestamp;

    public static volatile SingularAttribute<CarState, BigDecimal> amount;

    public static volatile SingularAttribute<CarState, Gate> inGate;

    public static volatile SingularAttribute<CarState, Gate> outGate;
}

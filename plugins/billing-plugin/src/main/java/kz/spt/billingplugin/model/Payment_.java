package kz.spt.billingplugin.model;

import kz.spt.lib.model.CarState;
import kz.spt.lib.model.Gate;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.math.BigDecimal;
import java.util.Date;

@StaticMetamodel(Payment.class)
public class Payment_ {
    public static volatile SingularAttribute<Payment, Long> id;

    public static volatile SingularAttribute<Payment, String> carNumber;

    public static volatile SingularAttribute<Payment, Date> created;

    public static volatile SingularAttribute<Payment, BigDecimal> price;

    public static volatile SingularAttribute<Payment, PaymentProvider> provider;
}

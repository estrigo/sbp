package kz.spt.billingplugin.model;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.math.BigDecimal;
import java.util.Date;

@StaticMetamodel(Transaction.class)
public class Transaction_ {

    public static volatile SingularAttribute<Transaction, Long> id;

    public static volatile SingularAttribute<Transaction, Date> date;

    public static volatile SingularAttribute<Transaction, String> plateNumber;

    public static volatile SingularAttribute<Transaction, BigDecimal> amount;

}

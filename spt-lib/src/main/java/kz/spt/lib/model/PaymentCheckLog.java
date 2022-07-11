package kz.spt.lib.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "payment_check_log")
@Audited
public class PaymentCheckLog {

    public PaymentCheckLog(String plateNumber, String message, BigDecimal summ, Long carStateId, PaymentCheckType paymentCheckType, BigDecimal currentBalance){
        this.plateNumber = plateNumber;
        this.message = message;
        this.summ = summ;
        this.carStateId = carStateId;
        this.paymentCheckType = paymentCheckType;
        this.currentBalance = currentBalance;
    }

    public enum PaymentCheckType{
        PREPAID,
        ABONEMENT,
        DEBT,
        NOT_FOUND,
        STANDARD;

        public static final PaymentCheckType[] ALL = {PREPAID, ABONEMENT, DEBT, NOT_FOUND, STANDARD};
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String plateNumber;

    @CreationTimestamp
    private Date created;

    private String message;

    private BigDecimal summ;

    private BigDecimal currentBalance;

    private Long carStateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_check_type")
    private PaymentCheckType paymentCheckType;
}

package kz.spt.billingplugin.model;

import kz.spt.api.model.Cars;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "payments", schema = "crm")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @CreationTimestamp
    private Date created;

    @UpdateTimestamp
    private Date updated;

    @Column ( name="AMOUNT", precision = 8, scale = 2 )
    private BigDecimal price;

    @Column(name = "tnx_id")
    String transaction;

    public enum Status {
        DRAFT,
        SUCCESS,
        ERROR
    }

    @Enumerated(EnumType.STRING)
    private Status status;

    @ManyToOne
    @JoinColumn(name = "provider_id")
    private PaymentProvider provider;

    String description;

    public enum Type {
        CREDIT,
        DEBIT,
    }

    @Enumerated(EnumType.STRING)
    private Type type;


    @Column(name = "car_number")
    String carNumber;

}

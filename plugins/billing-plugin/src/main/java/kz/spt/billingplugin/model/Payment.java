package kz.spt.billingplugin.model;

import kz.spt.lib.model.Customer;
import kz.spt.lib.model.Parking;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Audited
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column ( name="AMOUNT", precision = 8, scale = 2 )
    private BigDecimal price;

    @Column(name = "tnx_id")
    String transaction;

    @ManyToOne
    @JoinColumn(name = "provider_id")
    private PaymentProvider provider;

    @ManyToOne
    @JoinColumn(name = "parking_id")
    private Parking parking;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "in_date")
    private Date inDate;

    @Column(name = "out_date")
    private Date outDate;

    private Long carStateId;

    String rateDetails;

    String description;

    @Column(name = "car_number")
    String carNumber;

    @CreationTimestamp
    private Date created;

    @UpdateTimestamp
    private Date updated;

    String checkNumber;

    String checkUrl;

    private boolean ikkm;

    @Column(columnDefinition = "bit default 0", nullable = false)
    private boolean canceled;

    private String cancelReason;

    @Column (name="discounted_amount", precision = 8, scale = 2)
    private BigDecimal discountedPrice;

    @Column(columnDefinition="integer default 0", nullable = false)
    private Integer discount;

}

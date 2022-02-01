package kz.spt.lib.model;


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
@Table(name = "car_state")
public class CarState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "car_number")
    private String carNumber;

    @Column(name = "in_timestamp")
    private Date inTimestamp;

    @Column(name = "out_timestamp")
    private Date outTimestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private Parking.ParkingType type;

    @Column ( name="rate_amount", precision = 8, scale = 2 )
    private BigDecimal rateAmount;

    @Column ( name="amount", precision = 8, scale = 2 )
    private BigDecimal amount;

    private Boolean paid = false; // Заезжает на платной основе?

    private Long paymentId;

    private String inChannelIp;

    private String outChannelIp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parking")
    private Parking parking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "in_gate")
    private Gate inGate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "in_barrier")
    private Barrier inBarrier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "out_gate")
    private Gate outGate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "out_barrier")
    private Barrier outBarrier;

    @Column(name = "payment_json", columnDefinition = "text")
    private String paymentJson;

    @Column(name = "whitelist_json", columnDefinition = "text")
    private String whitelistJson;

    @Column(name = "booking_json", columnDefinition = "text")
    private String bookingJson;

    @Column(name = "zerotouch_json", columnDefinition = "text")
    private String zerotouchJson;

    private Boolean cashlessPayment = false;

    @CreationTimestamp
    private Date created;

    @UpdateTimestamp
    private Date updated;
}

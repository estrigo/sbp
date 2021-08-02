package kz.spt.api.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.util.Date;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    private Long amount;

    private String inChannelIp;

    private String outChannelIp;

    @ManyToOne
    @JoinColumn(name = "parking")
    private Parking parking;

    @ManyToOne
    @JoinColumn(name = "in_gate")
    private Gate inGate;

    @ManyToOne
    @JoinColumn(name = "in_barrier")
    private Barrier inBarrier;

    @ManyToOne
    @JoinColumn(name = "out_gate")
    private Gate outGate;

    @ManyToOne
    @JoinColumn(name = "out_barrier")
    private Barrier outBarrier;

    private Boolean paid = false;

    private Long payment;

    @CreationTimestamp
    private Date created;

    @UpdateTimestamp
    private Date updated;
}

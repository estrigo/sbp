package kz.spt.rateplugin.model;

import kz.spt.lib.model.Parking;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.util.Date;

/*
 * Java Entity to keep currency rate for a parking
 *
 * @Author:
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "parking_rate", schema = "crm")
public class ParkingRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parking")
    private Parking parking;

    private String name;

    @Column(name = "before_free_minutes")
    private int beforeFreeMinutes = 15;  //  Бесплатные минуты до оплаты

    @Column(name = "after_free_minutes")
    private int afterFreeMinutes = 15; //  Бесплатные минуты после оплаты для вьезда

    @Column(name = "cash_payment_value")
    private int cashPaymentValue;

    @Column(name = "online_payment_value")
    private int onlinePaymentValue;

    @CreationTimestamp
    private Date created;

    @UpdateTimestamp
    private Date updated;

    private String createdUser;

    private String updatedUser;
}

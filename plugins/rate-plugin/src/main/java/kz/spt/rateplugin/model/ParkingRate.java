package kz.spt.rateplugin.model;

import kz.spt.lib.model.Parking;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.Audited;

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
//@Audited
@Table(name = "parking_rate")
public class ParkingRate {

    public enum RateType {
        STANDARD,   // Одинаковая сумма для всех часов.  например 200 тенге за каждый час
        PROGRESSIVE, // Сумма за каждый час увеличивается или уменьшается. например 1 час 200mг, второй час 300тг, дальше по 400тг
        INTERVAL, // Разные суммы в зависимости от часов заезд. например каждый час между 08.00 - 16.00 по 500тг, между 16.00 - 08.00 по 1000тг
        PREPAID // Работает по предоплате
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parking")
    private Parking parking;

    @Enumerated(EnumType.STRING)
    @Column(name = "rate_type")
    private RateType rateType;

    private String name;

    @Column(name = "before_free_minutes")
    private Integer beforeFreeMinutes = 15;  //  Бесплатные минуты до оплаты

    @Column(name = "after_free_minutes")
    private Integer afterFreeMinutes = 15; //  Бесплатные минуты после оплаты для вьезда

    @Column(name = "cash_payment_value")
    private Integer cashPaymentValue;

    @Column(name = "online_payment_value")
    private Integer onlinePaymentValue;

    @Column(name = "day_payment_value")
    private Integer dayPaymentValue;

    @Column(name = "prepaid_value")
    private Integer prepaidValue;

    @Column(name = "progressive_json", columnDefinition = "text")
    private String progressiveJson;

    @Column(name = "interval_json", columnDefinition = "text")
    private String intervalJson;

    @CreationTimestamp
    private Date created;

    @UpdateTimestamp
    private Date updated;

    private String createdUser;

    private String updatedUser;
}

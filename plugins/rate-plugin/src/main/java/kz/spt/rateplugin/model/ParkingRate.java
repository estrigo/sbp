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
        DIMENSIONS, //тариф по габаритам - легковые,микроавтобусы, грузовики
        PREPAID, // Работает по предоплате
        FREE // Бесплатный тарифный план. Как и стандарт, только оплата равна 0.
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

    @Column(name = "cash_payment_passenger")
    private Integer cashPaymentValuePassenger;

    @Column(name = "online_payment_passenger")
    private Integer onlinePaymentValuePassenger;

    @Column(name = "cash_payment_van")
    private Integer cashPaymentValueVan;

    @Column(name = "online_payment_van")
    private Integer onlinePaymentValueVan;

    @Column(name = "cash_payment_truck")
    private Integer cashPaymentValueTruck;

    @Column(name = "online_payment_truck")
    private Integer onlinePaymentValueTruck;

    @Column(name = "cash_payment_passenger_night")
    private Integer cashPaymentValuePassengerNight;

    @Column(name = "online_payment_passenger_night")
    private Integer onlinePaymentValuePassengerNight;

    @Column(name = "cash_payment_van_night")
    private Integer cashPaymentValueVanNight;

    @Column(name = "online_payment_van_night")
    private Integer onlinePaymentValueVanNight;

    @Column(name = "cash_payment_truck_night")
    private Integer cashPaymentValueTruckNight;

    @Column(name = "online_payment_truck_night")
    private Integer onlinePaymentValueTruckNight;

    @Column(name = "day_payment_value")
    private Integer dayPaymentValue;

    @Column(name = "more_hours_calc_in_days", columnDefinition="tinyint(1) default 1")
    private Boolean moreHoursCalcInDays = false;

    @Column(name = "prepaid_value")
    private Integer prepaidValue; //Сумма предоплаты

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

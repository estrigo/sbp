package kz.spt.billingplugin.model;

import kz.spt.lib.model.Cars;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
@Table(name = "transaction", indexes = {
        @Index(name = "date_idx", columnList = "date")
})
public class Transaction {

    public Transaction(String plateNumber, BigDecimal amount, Long carStateId, String description, String descriptionRu,
                       String provider, BigDecimal remainder, Cars car){
        this.plateNumber = plateNumber;
        this.amount = amount;
        this.carStateId = carStateId;
        this.description = description;
        this.descriptionRu = descriptionRu;
        this.date = new Date();
        this.provider = provider;
        this.remainder = remainder;
        this.car = car;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "plate_number")
    String plateNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car")
    private Cars car;

    @Column ( name="AMOUNT", precision = 8, scale = 2 )
    private BigDecimal amount;

    @Column(name = "description")
    String description;

    @Column(name = "car_state_id")
    Long carStateId;

    @Column(name = "description_ru")
    String descriptionRu;

    @Column(name = "date")
    private Date date;

    @Column(name = "provider")
    private String provider;

    @Column(name = "remainder")
    private BigDecimal remainder;
}
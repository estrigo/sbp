package kz.spt.billingplugin.model;

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
@Table(name = "transaction")
public class Transaction {

    public Transaction(String plateNumber, BigDecimal amount, String description, String descriptionRu){
        this.plateNumber = plateNumber;
        this.amount = amount;
        this.description = description;
        this.descriptionRu = descriptionRu;
        this.date = new Date();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "plate_number")
    String plateNumber;

    @Column ( name="AMOUNT", precision = 8, scale = 2 )
    private BigDecimal amount;

    @Column(name = "description")
    String description;

    @Column(name = "description_ru")
    String descriptionRu;


    @Column(name = "date")
    private Date date;
}
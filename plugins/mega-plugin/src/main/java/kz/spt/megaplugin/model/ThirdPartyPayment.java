package kz.spt.megaplugin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "third_party_payments")
public class ThirdPartyPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    private String carNumber;

    @Column(name = "entry_date")
    private Date entryDate;

    @Column(name = "exit_date")
    private Date exitDate;

    @Column ( name="rate_amount", precision = 8, scale = 2 )
    private BigDecimal rateAmount;

    @Column(name="parkingUid")
    String parkingUID;

    @Column(name="sent")
    private Boolean sent;
}

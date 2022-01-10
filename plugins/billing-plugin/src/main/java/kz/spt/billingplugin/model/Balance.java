package kz.spt.billingplugin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Audited
@Table(name = "balance")
public class Balance {

    @Id
    @Column(name = "plate_number")
    private String plateNumber;

    @Column ( name="balance", precision = 8, scale = 2 )
    private BigDecimal balance;
}

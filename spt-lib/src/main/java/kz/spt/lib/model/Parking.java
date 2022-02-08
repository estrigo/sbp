package kz.spt.lib.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.util.Date;
import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Audited
@Table(name = "parking")
public class Parking {

    public enum ParkingType {
        WHITELIST,
        PAYMENT,
        WHITELIST_PAYMENT,
        PREPAID;

        public static final ParkingType[] ALL = {WHITELIST, PAYMENT, WHITELIST_PAYMENT, PREPAID};
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String description;

    @Size(min = 3)
    private String name;

    private Integer parkingSpaceNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "parking_type")
    private ParkingType parkingType;

    @OneToMany(mappedBy = "parking")
    private List<Gate> gateList;

    @CreationTimestamp
    private Date created;

    @UpdateTimestamp
    private Date updated;
}

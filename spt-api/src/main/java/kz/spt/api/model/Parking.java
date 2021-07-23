package kz.spt.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "parking")
public class Parking {

    public enum ParkingType {
        WHITELIST,
        PAYMENT,
        WHITELIST_PAYMENT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String description;

    private String name;

    private Integer parkingSpaceNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "parking_type")
    private ParkingType parkingType;

    @OneToMany(mappedBy = "parking")
    private List<Gate> gateList;
}

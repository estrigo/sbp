package kz.spt.rateplugin.model;

import lombok.*;

import javax.persistence.*;
import java.util.List;

@Entity
@Data
@Table(name = "interval_rate")
public class IntervalRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String datetimeFrom;

    private String datetimeTo;

    @OneToMany(mappedBy = "intervalRate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RateCondition> rateConditions;

    @ManyToOne(fetch = FetchType.EAGER)
    private ParkingRate parkingRate;
}

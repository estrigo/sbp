package kz.spt.rateplugin.model;

import kz.spt.lib.model.Dimensions;
import lombok.Data;

import javax.persistence.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Data
@Table(name = "interval_rate")
public class IntervalRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String datetimeFrom;

    @Column
    private String datetimeTo;

    @OneToMany(mappedBy = "intervalRate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RateCondition> rateConditions;

    @ManyToOne(fetch = FetchType.EAGER)
    private ParkingRate parkingRate;

    @ManyToMany(cascade = CascadeType.MERGE, fetch = FetchType.EAGER)
    @JoinTable(
            name = "interval_rate_dimensions",
            joinColumns = @JoinColumn(name = "interval_rate_id"),
            inverseJoinColumns = @JoinColumn(name = "dimensions_id")
    )
    private Set<Dimensions> dimensionSet = new HashSet<>();
}

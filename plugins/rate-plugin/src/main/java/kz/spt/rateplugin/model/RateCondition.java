package kz.spt.rateplugin.model;

import lombok.*;

import javax.persistence.*;

@Entity
@Data
@Table(name = "rate_condition")
public class RateCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interval_rate_id")
    private IntervalRate intervalRate;

    private String intervalType;

    private Integer onlineRate;

    private Integer parkomatRate;

}

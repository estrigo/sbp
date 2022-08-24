package kz.spt.rateplugin.model;

import lombok.*;

import javax.persistence.*;

@Entity
@Data
@Table(name = "rate_condition")
public class RateCondition {

    public enum IntervalType {
        minutes,
        hour,
        allNext,
        entrance;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interval_rate_id")
    private IntervalRate intervalRate;

    @Enumerated(EnumType.STRING)
    private IntervalType intervalType;

    private Integer standing;

    private Integer onlineRate;

    private Integer parkomatRate;

}

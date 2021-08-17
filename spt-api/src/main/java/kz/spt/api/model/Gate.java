package kz.spt.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "gate")
public class Gate {

    public enum GateType {
        IN,
        OUT
//        REVERSE
        ;

        public static final Gate.GateType[] ALL = {IN, OUT};
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "gate_type")
    private GateType gateType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parking")
    private Parking parking;

    @OneToMany(mappedBy = "gate")
    private List<Camera> cameraList;

    @OneToOne(mappedBy = "gate")
    private Barrier barrier;

    @OneToOne(mappedBy = "gate")
    private Controller controller;

    @CreationTimestamp
    private Date created;

    @UpdateTimestamp
    private Date updated;
}

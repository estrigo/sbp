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
@Table(name = "gate")
public class Gate {

    public enum GateType {
        IN,
        OUT,
//        REVERSE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "gate_type")
    private GateType gateType;

    @ManyToOne
    @JoinColumn(name = "parking")
    private Parking parking;

    @OneToMany(mappedBy = "gate")
    private List<Camera> cameraList;

    @OneToOne(mappedBy = "gate")
    private Barrier barrier;

    @OneToOne(mappedBy = "controller")
    private Controller controller;
}

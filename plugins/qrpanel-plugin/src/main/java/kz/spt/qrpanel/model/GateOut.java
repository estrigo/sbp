package kz.spt.qrpanel.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import javax.persistence.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "gate")
public class GateOut {

    public enum GateType {
        OUT
    }

    @Id
    @Column(name = "id", insertable=false,updatable = false)
    private Long id;

    @Column(insertable=false,updatable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "gate_type", insertable=false,updatable = false)
    private GateType gateType;


}

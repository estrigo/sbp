package kz.spt.lib.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.Date;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Audited
@Table(name = "barrier")
public class Barrier {

    public enum BarrierType {
        MODBUS,
        SNMP;

        public static final Barrier.BarrierType[] ALL = {SNMP, MODBUS};
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;

    private String ip;

    private String password;

    private Integer snmpVersion;

    @Column(name = "open_oid")
    private String openOid;

    @Column(name = "close_oid")
    private String closeOid;

    @Enumerated(EnumType.STRING)
    @Column(name = "barrier_type")
    private Barrier.BarrierType barrierType;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gate")
    private Gate gate;

    // --- Данные петли для проверки присуствия машины
    @Enumerated(EnumType.STRING)
    @Column(name = "loop_type")
    private Barrier.BarrierType loopType;

    private String loopIp;

    private String loopPassword;

    private String loopOid;

    private Integer loopSnmpVersion;

    private Integer loopDefaultValue;

    @CreationTimestamp
    private Date created;

    @UpdateTimestamp
    private Date updated;
}

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

    public enum SensorsType {
        MANUAL,
        AUTOMATIC;

        public static final Barrier.SensorsType[] ALL = { MANUAL, AUTOMATIC};
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

    @Column(name = "modbus_open_register")
    private Integer modbusOpenRegister;

    @Column(name = "modbus_close_register")
    private Integer modbusCloseRegister;

    @Enumerated(EnumType.STRING)
    @Column(name = "sensors_type")
    private Barrier.SensorsType sensorsType;


    // --- Данные петли для проверки присуствия машины  (После шлагбаума)
    @Enumerated(EnumType.STRING)
    @Column(name = "before_loop_type")
    private Barrier.BarrierType loopType;

    private String loopIp;

    private String loopPassword;

    private String loopOid;

    private Integer loopSnmpVersion;

    private Integer loopDefaultValue = 0;

    @Column(name = "loop_modbus_register")
    private Integer loopModbusRegister;

    // --- Данные фотоэлемента для проверки присуствия машины (До шлагбаума)
    @Enumerated(EnumType.STRING)
    @Column(name = "loop_type")
    private Barrier.BarrierType photoElementType;

    private String photoElementIp;

    private String photoElementPassword;

    private String photoElementOid;

    private Integer photoElementSnmpVersion;

    private Integer photoElementDefaultValue = 0;

    @Column(name = "photo_element_modbus_register")
    private Integer photoElementModbusRegister;

    @CreationTimestamp
    private Date created;

    @UpdateTimestamp
    private Date updated;
}

package kz.spt.lib.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Audited
@Table(name = "emergency_signal_config")
public class EmergencySignalConfig {

    @Id
    private String ip;

    @Column(name = "modbus_register")
    private Integer modbusRegister;

    private Integer modbusSosActiveValue = 1;
}

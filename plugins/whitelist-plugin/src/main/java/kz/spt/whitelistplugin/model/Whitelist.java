package kz.spt.whitelistplugin.model;

import kz.spt.lib.model.Cars;
import kz.spt.lib.model.Parking;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.util.Date;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "whitelist", schema = "crm")
public class Whitelist extends AbstractWhitelist {

    @OneToOne(optional=false)
    @JoinColumn(name = "car_id")
    private Cars car;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private WhitelistGroups group;

    @Transient
    private String platenumber;

    @Transient
    private Long groupId;
}

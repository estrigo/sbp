package kz.spt.whitelistplugin.model;

import kz.spt.lib.model.Cars;
import lombok.*;
import org.hibernate.annotations.Proxy;
import org.hibernate.envers.Audited;

import javax.persistence.*;

@Entity
@Data
@Audited
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "whitelist", uniqueConstraints = @UniqueConstraint(columnNames={"car_id", "parking_id"}))
@Proxy(lazy = false)
public class Whitelist extends AbstractWhitelist {

    @ManyToOne
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

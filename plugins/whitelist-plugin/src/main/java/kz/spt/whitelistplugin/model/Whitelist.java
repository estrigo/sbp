package kz.spt.whitelistplugin.model;

import kz.spt.lib.model.Cars;
import lombok.*;
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
@Table(name = "whitelist", schema = "crm")
public class Whitelist {

    public enum Type {
        PERIOD,
        UNLIMITED;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne(optional=false)
    @JoinColumn(name = "car_id")
    private Cars car;

    private Date access_start;

    private Date access_end;

    @Enumerated(EnumType.STRING)
//    @Column(name = "type")
    private Whitelist.Type type;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private WhitelistGroups group;

    @CreationTimestamp
    private Date created;

    @UpdateTimestamp
    private Date updated;

    private String createdUser;

    private String updatedUser;

    @Transient
    private String platenumber;

    @Transient
    private Long groupId;

    @Transient
    private String accessStartString;

    @Transient
    private String accessEndString;
}

package kz.spt.whitelistplugin.model;


import kz.spt.lib.model.Cars;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "whitelist_group", schema = "crm")
public class WhitelistGroups {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    private String name;

    @ManyToMany
    @JoinTable(
            name = "whitelist_groups_cars",
            joinColumns = {@JoinColumn(name = "groups_id")},
            inverseJoinColumns = {@JoinColumn(name = "cars_id")}
    )
    Set<Cars> cars = new HashSet<>();

    @CreationTimestamp
    private Date created;

    @UpdateTimestamp
    private Date updated;

    private String updatedUser;
}

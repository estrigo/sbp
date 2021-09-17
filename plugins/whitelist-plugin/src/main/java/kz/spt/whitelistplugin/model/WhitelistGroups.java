package kz.spt.whitelistplugin.model;


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
@Table(name = "whitelist_group", schema = "crm")
public class WhitelistGroups {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private Whitelist.Type type;

    private Date access_start;

    private Date access_end;

    @OneToMany(mappedBy = "group")
    private List<Whitelist> whitelists;

    @CreationTimestamp
    private Date created;

    @UpdateTimestamp
    private Date updated;

    private String updatedUser;

    @Transient
    List<String> plateNumbers;

    @Transient
    private String accessStartString;

    @Transient
    private String accessEndString;
}

package kz.spt.whitelistplugin.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.List;

@Entity
@Data
@Audited
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "whitelist_group", schema = "crm")
public class WhitelistGroups extends AbstractWhitelist {

    private String name;

    private Integer size;

    @OneToMany(mappedBy = "group")
    private List<Whitelist> whitelists;

    @Transient
    List<String> plateNumbers;

    @Transient
    Boolean forceUpdate = false;
}

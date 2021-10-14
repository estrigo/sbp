package kz.spt.whitelistplugin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
public class AbstractWhitelist {

    public enum Type {
        PERIOD,
        UNLIMITED,
        CUSTOM;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    private Date access_start;

    private Date access_end;

    @Enumerated(EnumType.STRING)
    private Type type;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private WhitelistCategory category;

    @Column(columnDefinition = "text")
    private String customJson;

    @CreationTimestamp
    private Date created;

    @UpdateTimestamp
    private Date updated;

    private String createdUser;

    private String updatedUser;

    @Transient
    private Long categoryId;

    @Transient
    private String accessStartString;

    @Transient
    private String accessEndString;

    @Transient
    private String conditionDetail;
}

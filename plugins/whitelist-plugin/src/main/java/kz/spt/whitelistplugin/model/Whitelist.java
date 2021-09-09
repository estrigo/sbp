package kz.spt.whitelistplugin.model;

import kz.spt.lib.model.Cars;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.util.Date;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "whitelist", schema = "crm")
public class Whitelist {

    public enum Type {
        ONCE,
        PERIOD,
        WEEKDAYS,
        UNLIMITED,
        MONTHLY;

        public static final Type[] ACTIVE = {UNLIMITED, PERIOD, MONTHLY, ONCE, WEEKDAYS};
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "car_id")
    private Cars car;

    private Date access_start;

    private Date access_end;

    @Enumerated(EnumType.STRING)
    private Type type;

    @ManyToOne
    @JoinColumn(name = "category")
    private Category category;

    @CreationTimestamp
    private Date created;

    @UpdateTimestamp
    private Date updated;

    private String createdUser;

    private String updatedUser;

    @Transient
    @Size(min = 3, max = 16)
    private String platenumber;

    @Transient
    private String accessStartString;

    @Transient
    private String accessEndString;
}

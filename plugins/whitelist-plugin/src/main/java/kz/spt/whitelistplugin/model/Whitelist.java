package kz.spt.whitelistplugin.model;

import crm.model.Cars;
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
public class Whitelist {

    public static enum Type {
        ONCE,
        PERIOD,
        WEEKDAYS,
        UNLIMITED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

    @Column(unique = true)
    private String number;

    @ManyToOne
    @JoinColumn(name = "car_id")
    private Cars car;

    private Date access_start;

    private Date access_end;

    @Enumerated(EnumType.STRING)
    private Type type;

    @CreationTimestamp
    private Date created;

    @UpdateTimestamp
    private Date updated;

    private String createdUser;

    private String updatedUser;
}

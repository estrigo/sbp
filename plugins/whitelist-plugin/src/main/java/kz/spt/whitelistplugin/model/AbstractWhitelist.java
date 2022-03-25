package kz.spt.whitelistplugin.model;

import kz.spt.lib.model.Parking;
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
        CUSTOM,
        BOTTAXI;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "parking_id")
    private Parking parking;

    private Date access_start;

    private Date access_end;

    @Enumerated(EnumType.STRING)
    private Type type;

    @Column(columnDefinition = "text")
    private String customJson;

    private String fullName;
    private String address;
    private String parkingNumber;
    private String comment;

    @CreationTimestamp
    private Date created;

    @UpdateTimestamp
    private Date updated;

    private String createdUser;

    private String updatedUser;

    @Transient
    private String accessStartString;

    @Transient
    private String accessEndString;

    @Transient
    private String conditionDetail;

    @Transient
    private Long parkingId;
}

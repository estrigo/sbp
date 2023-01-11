package kz.spt.abonomentplugin.model;

import kz.spt.lib.model.Cars;
import kz.spt.lib.model.Parking;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Audited
@Table(name = "abonoment")
public class Abonement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "car_id")
    private Cars car;

    @Column(name = "begin")
    private Date begin;

    @Column(name = "end")
    private Date end;

    @Column(name = "months")
    private Integer months;

    @Column(name = "paid")
    private Boolean paid;

    @Column(name = "price")
    private BigDecimal price;

    @Column(name = "checked")
    private Boolean checked;

    @Column(name = "extended")
    private Boolean extended = false;

    @Column(name = "type")
    private String type;

    @Column(name = "paidType")
    private String paidType;

    @Column(columnDefinition = "text")
    private String customNumbers;

    @ManyToOne
    @JoinColumn(name = "parking_id")
    private Parking parking;

    @CreationTimestamp
    private Date created;

    @UpdateTimestamp
    private Date updated;
}

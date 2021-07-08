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

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

    @CreationTimestamp
    private Date created;

    @UpdateTimestamp
    private Date updated;

    @Column(unique = true)
    private String number;

    @OneToOne
    @JoinColumn(name = "car_id")
    private Cars car;

    private Date access_end;


}

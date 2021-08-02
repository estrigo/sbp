package kz.spt.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.util.Date;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "barrier")
public class Barrier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;

    @Column(unique=true)
    private String ip;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gate")
    private Gate gate;

    @CreationTimestamp
    private Date created;

    @UpdateTimestamp
    private Date updated;
}

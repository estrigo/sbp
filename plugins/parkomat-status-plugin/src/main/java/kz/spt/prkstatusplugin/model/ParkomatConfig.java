package kz.spt.prkstatusplugin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;

import javax.persistence.*;
import java.util.Date;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "parkomat_config")
public class ParkomatConfig {

    @Id
    private String ip;

    @Column(name="config", columnDefinition="TEXT")
    private String config;

    @CreationTimestamp
    private Date created;

    @UpdateTimestamp
    private Date updated;

}

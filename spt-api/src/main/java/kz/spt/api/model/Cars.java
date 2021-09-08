package kz.spt.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.Audited;
import org.hibernate.validator.constraints.Length;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.util.Date;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Audited
@Table(name = "cars")
public class Cars {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique=true)
    @Size(min = 3, max = 16)
    private String platenumber;

    private String color;

    private String brand;

    private Boolean deleted = false;

    @CreationTimestamp
    private Date created;

    @UpdateTimestamp
    private Date updated;

    public String getNullSafeBrand(){
        return getBrand() != null ? getBrand() : "";
    }

    public String getNullSafeColor(){
        return getColor() != null ? getColor() : "";
    }
}

package kz.spt.megaplugin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.common.aliasing.qual.Unique;

import javax.persistence.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "third_party_cars")
public class ThirdPartyCars {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Unique
    private String car_number;

    private String type; // direct: акцептное списание, indirect: безакцептное списание

    private Boolean status; // true: активный, false: заморожен
}

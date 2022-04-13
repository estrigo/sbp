package kz.spt.lib.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.common.aliasing.qual.Unique;
import org.hibernate.envers.Audited;

import javax.persistence.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Unique
    private String model;
    private Integer type; //types: 1 - passenger car	1, gazelle	2, truck 3

}

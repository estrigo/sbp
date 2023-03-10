package kz.spt.abonomentplugin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Audited
@Table(name = "abonoment_type")
public class AbonomentTypes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "period")
    private Integer period;


    @Column(columnDefinition = "text")
    private String customJson;

    @Column(name = "type")
    private String type;

    @Column(name = "price")
    private Integer price = 0;

    @Column(columnDefinition = "text")
    private String customNumbers;

    @Transient
    private String description;

    @Column(name = "created_user")
    private String createdUser;
}

package kz.spt.lib.model;


import lombok.Data;
import javax.persistence.*;

@Entity
@Data
@Table(name = "property")
public class Property {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "prop_key", nullable = false)
    private String key;

    @Column(name = "prop_value", nullable = false)
    private String value;

    private Boolean disabled;
}

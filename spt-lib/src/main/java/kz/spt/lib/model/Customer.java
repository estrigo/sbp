package kz.spt.lib.model;

import lombok.*;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.List;


@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Audited
@Table(name = "customers", schema = "crm")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;

    private String lastName;

    private String phoneNumber;


    @OneToMany(mappedBy = "customer")
    private List<Cars> cars;


    @Transient
    List<String> plateNumbers;





}

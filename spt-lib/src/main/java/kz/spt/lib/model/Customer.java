package kz.spt.lib.model;

import lombok.*;
import org.hibernate.envers.Audited;
import org.hibernate.validator.constraints.Email;

import javax.persistence.*;
import java.util.List;


@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Audited
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;

    private String lastName;

    @Column(unique=true)
    private String phoneNumber;

    @OneToMany(mappedBy = "customer")
    private List<Cars> cars;

    @Transient
    List<String> plateNumbers;

    @Column(name = "email", unique = true)
    @Email(message = "Please provide a valid e-mail")
    private String email;

    @Column
    private Boolean mailReceiver;

}

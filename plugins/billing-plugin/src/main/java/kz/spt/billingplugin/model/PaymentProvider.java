package kz.spt.billingplugin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.envers.Audited;

import javax.persistence.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Audited
@Table(name = "payment_provider", schema = "crm")
public class PaymentProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    private String provider;

    private String name;

    private Boolean enabled = true;

    @Column(name = "client_id", unique=true)
    private String clientId;

    private String secret;

    @Transient
    private String password;
}
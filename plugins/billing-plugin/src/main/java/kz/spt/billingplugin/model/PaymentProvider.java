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
@Table(name = "payment_provider")
public class PaymentProvider extends PaymentProviderAbstract {
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

    private Boolean cashlessPayment = false;

    @Transient
    private String password;

    /* Для Вебкассы - ID кассы, для ReKassa API TOKEN */
    String webKassaID;

    /*  */
    String webKassaLogin;

    String webKassaPassword;
    /* IP-адрес паркомата */
    String parkomatIP;

    private Boolean isParkomat = false;
}
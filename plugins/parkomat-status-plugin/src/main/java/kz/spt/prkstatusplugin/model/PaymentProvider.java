package kz.spt.prkstatusplugin.model;


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
    @Column(name = "id", insertable=false,updatable = false)
    private Long id;

    @Column(insertable=false,updatable = false)
    private String provider;

    @Column(insertable=false,updatable = false)
    private String name;

    @Column(insertable=false,updatable = false)
    private Boolean enabled = true;

    @Column(name = "client_id", unique=true,insertable=false,updatable = false)
    private String clientId;

    @Column(insertable=false,updatable = false)
    private String secret;

    @Column(insertable=false, updatable=false)
    private Boolean cashlessPayment = false;

    @Column(insertable=false, updatable=false)
    String webKassaID;

    @Column(insertable=false, updatable=false)
    String webKassaLogin;

    @Column(insertable=false, updatable=false)
    String webKassaPassword;

    @Column(insertable=false, updatable=false)
    String parkomatIP;

    @Column(insertable=false, updatable=false)
    private Boolean isParkomat = false;
}
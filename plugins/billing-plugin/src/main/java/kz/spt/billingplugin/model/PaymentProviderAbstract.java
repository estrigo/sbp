package kz.spt.billingplugin.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;

@Data
@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
public class PaymentProviderAbstract {
    public enum OFD_PROVIDER_TYPE {
        WebKassa,
        ReKassa
    }

    @Enumerated(EnumType.STRING)
    private OFD_PROVIDER_TYPE ofdProviderType = OFD_PROVIDER_TYPE.WebKassa;
}

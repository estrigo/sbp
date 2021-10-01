package kz.spt.billingplugin.dto;

import kz.spt.billingplugin.model.PaymentProvider;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data @NoArgsConstructor
public class PaymentProviderDTO {

    public Long id;
    public String provider;
    public String name;
    public String clientId;
    public String enabled;

    public static PaymentProviderDTO convertToDto(PaymentProvider paymentProvider) {
        PaymentProviderDTO paymentProviderDTO = new PaymentProviderDTO();
        paymentProviderDTO.setId(paymentProvider.getId());
        paymentProviderDTO.setProvider(paymentProvider.getProvider());
        paymentProviderDTO.setName( paymentProvider.getName());
        paymentProviderDTO.setClientId(paymentProvider.getClientId());
        paymentProviderDTO.setEnabled(paymentProvider.getEnabled() != null && paymentProvider.getEnabled() ? "Нет" : "Да");
        return paymentProviderDTO;
    }
}

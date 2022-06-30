package kz.spt.billingplugin.dto;

import kz.spt.billingplugin.model.PaymentProvider;
import kz.spt.lib.model.CurrentUser;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Arrays;


@Data @NoArgsConstructor
public class PaymentProviderDTO {

    public Long id;
    public String provider;
    public String name;
    public String clientId;
    public String enabled;
    public String cashlessPayment;
    public boolean parkomat;

    public static PaymentProviderDTO convertToDto(PaymentProvider paymentProvider) {

        PaymentProviderDTO paymentProviderDTO = new PaymentProviderDTO();
        paymentProviderDTO.setId(paymentProvider.getId());
        paymentProviderDTO.setProvider(paymentProvider.getProvider());
        paymentProviderDTO.setName( paymentProvider.getName());
        paymentProviderDTO.setClientId(paymentProvider.getClientId());
        paymentProviderDTO.setEnabled(paymentProvider.getEnabled() != null && paymentProvider.getEnabled() ? "Allowed" : "Not Allowed");
        paymentProviderDTO.setCashlessPayment(paymentProvider.getCashlessPayment() != null && paymentProvider.getCashlessPayment() ? "Allowed" : "Not Allowed");
        paymentProviderDTO.setParkomat(paymentProvider.getIsParkomat() != null ? paymentProvider.getIsParkomat() : false);
        return paymentProviderDTO;
    }
}

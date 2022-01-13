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
    public Boolean canEdit = true;
    public String cashlessPayment;

    public static PaymentProviderDTO convertToDto(PaymentProvider paymentProvider, UserDetails currentUser) {

        boolean canEdit = currentUser.getAuthorities().stream().anyMatch(m-> Arrays.asList("ROLE_SUPERADMIN","ROLE_ADMIN","ROLE_MANAGER").contains(m.getAuthority()));

        PaymentProviderDTO paymentProviderDTO = new PaymentProviderDTO();
        paymentProviderDTO.setId(paymentProvider.getId());
        paymentProviderDTO.setProvider(paymentProvider.getProvider());
        paymentProviderDTO.setName( paymentProvider.getName());
        paymentProviderDTO.setClientId(paymentProvider.getClientId());
        paymentProviderDTO.setCanEdit(canEdit);
        paymentProviderDTO.setEnabled(paymentProvider.getEnabled() != null && paymentProvider.getEnabled() ? "Allowed" : "Not Allowed");
        paymentProviderDTO.setCashlessPayment(paymentProvider.getCashlessPayment() != null && paymentProvider.getCashlessPayment() ? "Allowed" : "Not Allowed");
        return paymentProviderDTO;
    }
}

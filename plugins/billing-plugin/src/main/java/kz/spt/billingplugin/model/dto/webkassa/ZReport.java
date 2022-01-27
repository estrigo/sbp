package kz.spt.billingplugin.model.dto.webkassa;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ZReport {
    @JsonProperty("Token")
    public String token;
    @JsonProperty("CashboxUniqueNumber")
    public String cashboxUniqueNumber;
}

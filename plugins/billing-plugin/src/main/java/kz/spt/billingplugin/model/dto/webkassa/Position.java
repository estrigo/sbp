package kz.spt.billingplugin.model.dto.webkassa;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;

import java.math.BigDecimal;

@ToString
public class Position{
    @JsonProperty("Count") 
    public int count = 1;
    @JsonProperty("Tax") 
    public BigDecimal tax;
    @JsonProperty("TaxType") 
    public long taxType;
    @JsonProperty("TaxPercent")
    public int taxPercent;
    @JsonProperty("PositionName") 
    public String positionName;
    @JsonProperty("PositionCode") 
    public String positionCode = "1";
    @JsonProperty("IsStorno") 
    public boolean isStorno;
    @JsonProperty("MarkupDeleted") 
    public boolean markupDeleted;
    @JsonProperty("DiscountDeleted") 
    public boolean discountDeleted;
    @JsonProperty("Price") 
    public int price;
}

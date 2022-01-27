package kz.spt.billingplugin.model.dto.webkassa;
import com.fasterxml.jackson.annotation.JsonProperty; 
public class Position{
    @JsonProperty("Count") 
    public int count = 1;
    @JsonProperty("Tax") 
    public int tax;
    @JsonProperty("TaxType") 
    public int taxType;
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

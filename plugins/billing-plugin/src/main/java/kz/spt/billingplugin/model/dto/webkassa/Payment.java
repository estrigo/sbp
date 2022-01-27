package kz.spt.billingplugin.model.dto.webkassa;
import com.fasterxml.jackson.annotation.JsonProperty; 
public class Payment{

    public static int TYPE_CARD = 1;
    public static int TYPE_CASH = 0;

    @JsonProperty("PaymentType") 
    public int paymentType = TYPE_CASH;
    @JsonProperty("Sum") 
    public String sum ;
}

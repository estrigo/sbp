package kz.spt.billingplugin.model.dto.webkassa;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;

@Data
public class Check{
    @JsonProperty("Token") 
    public String token;
    @JsonProperty("CashboxUniqueNumber") 
    public String cashboxUniqueNumber;
    @JsonProperty("OperationType") 
    public int operationType = 2;
    @JsonProperty("Positions") 
    public ArrayList<Position> positions = new ArrayList<>();
    @JsonProperty("Payments") 
    public ArrayList<Payment> payments = new ArrayList<>();
    @JsonProperty("RoundType") 
    public int roundType;
    @JsonProperty("Change") 
    public String change;
    @JsonProperty("ExternalCheckNumber") 
    public String externalCheckNumber;
}

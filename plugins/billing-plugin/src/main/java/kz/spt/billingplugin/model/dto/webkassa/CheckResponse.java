package kz.spt.billingplugin.model.dto.webkassa;
import com.fasterxml.jackson.annotation.JsonProperty;

@lombok.Data
public class CheckResponse{
    @JsonProperty("Data") 
    public Data data;
}

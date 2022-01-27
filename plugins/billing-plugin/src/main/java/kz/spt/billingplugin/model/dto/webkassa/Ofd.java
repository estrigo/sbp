package kz.spt.billingplugin.model.dto.webkassa;
import com.fasterxml.jackson.annotation.JsonProperty; 
public class Ofd{
    @JsonProperty("Name") 
    public String name;
    @JsonProperty("Host") 
    public String host;
    @JsonProperty("Code") 
    public int code;
}

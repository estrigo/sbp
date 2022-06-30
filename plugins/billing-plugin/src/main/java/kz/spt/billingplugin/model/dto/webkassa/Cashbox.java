package kz.spt.billingplugin.model.dto.webkassa;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;

@ToString
public class Cashbox{
    @JsonProperty("UniqueNumber") 
    public String uniqueNumber;
    @JsonProperty("RegistrationNumber") 
    public String registrationNumber;
    @JsonProperty("IdentityNumber") 
    public String identityNumber;
    @JsonProperty("Address") 
    public String address;
    @JsonProperty("Ofd") 
    public Ofd ofd;
}

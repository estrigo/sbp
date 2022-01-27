package kz.spt.billingplugin.model.dto.webkassa;
import com.fasterxml.jackson.annotation.JsonProperty; 
public class Data{
    @JsonProperty("CheckNumber") 
    public String checkNumber;
    @JsonProperty("DateTime") 
    public String dateTime;
    @JsonProperty("OfflineMode") 
    public boolean offlineMode;
    @JsonProperty("CashboxOfflineMode") 
    public boolean cashboxOfflineMode;
    @JsonProperty("Cashbox") 
    public Cashbox cashbox;
    @JsonProperty("CheckOrderNumber") 
    public int checkOrderNumber;
    @JsonProperty("ShiftNumber") 
    public int shiftNumber;
    @JsonProperty("EmployeeName") 
    public String employeeName;
    @JsonProperty("TicketUrl") 
    public String ticketUrl;
    @JsonProperty("TicketPrintUrl") 
    public String ticketPrintUrl;
}

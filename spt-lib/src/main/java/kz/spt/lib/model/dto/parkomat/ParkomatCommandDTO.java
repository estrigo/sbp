package kz.spt.lib.model.dto.parkomat;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * <h1>Команда поступающая от паркомата</h1>
 *
 * @author Kairzhan
 */
@Data
public class ParkomatCommandDTO {
    //Plate number
    @Getter(AccessLevel.NONE)
    String account;
    //Transaction id from parkomat
    String txn_id;
    //Command name
    String command;

    //Payment amount
    BigDecimal sum;

    //Payment type like cash or ikkm
    String type;

    // Parkomat provider name
    String parkomat;

    //Amount of change payed by cash
    BigDecimal change;


    /**
     * Platenumber in upper case
     * @return plate number in upper case
     */
    public String getAccount() {
        return account!=null ? account.toUpperCase(Locale.ROOT) : "";
    }
}


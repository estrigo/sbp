package kz.spt.lib.model.dto.parkomat;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Locale;

@Data
public class ParkomatCommandDTO {

    @Getter(AccessLevel.NONE)
    String account;

    String txn_id;
    String command;
    BigDecimal sum;
    String type;
    String parkomat;
    BigDecimal change;
    String paymentType;

    public String getAccount() {
        return account!=null ? account.toUpperCase(Locale.ROOT) : "";
    }
}


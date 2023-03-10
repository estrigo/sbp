package kz.spt.lib.model.dto.payment;

import lombok.Data;

import java.math.BigDecimal;
@Data
public class CommandDto {

    public String command;
    public String account;
    public String txn_id;
    public BigDecimal sum;
    public Boolean prepaid = false;
    public String clientId;
    public Integer service_id;
    public BigDecimal discount;
    public BigDecimal discountedSum;
}

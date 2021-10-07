package kz.spt.lib.model.dto.payment;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;

public class CommandDto {

    public String command;
    public String account;
    public String txn_id;
    public BigDecimal sum;
}

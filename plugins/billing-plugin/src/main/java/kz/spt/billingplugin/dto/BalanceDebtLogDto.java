package kz.spt.billingplugin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import kz.spt.billingplugin.model.BalanceDebtLog;

import java.math.BigDecimal;
import java.util.Date;

public class BalanceDebtLogDto {

    public Long id;
    @JsonFormat(pattern="dd.MM.yyyy HH:mm:ss")
    public Date date;
    public String plateNumber;
    public BigDecimal balance;

    public static BalanceDebtLogDto fromBalanceDebtLog(BalanceDebtLog balanceDebtLog){
        BalanceDebtLogDto balanceDebtLogDto = new BalanceDebtLogDto();
        balanceDebtLogDto.id = balanceDebtLog.getId();
        balanceDebtLogDto.plateNumber = balanceDebtLog.getPlateNumber();
        balanceDebtLogDto.date = balanceDebtLog.getCreated();
        balanceDebtLogDto.balance = balanceDebtLog.getBalance();
        return balanceDebtLogDto;
    }
}

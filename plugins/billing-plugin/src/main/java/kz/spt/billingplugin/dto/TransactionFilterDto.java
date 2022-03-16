package kz.spt.billingplugin.dto;

import kz.spt.billingplugin.model.Transaction;

import java.math.BigDecimal;

public class TransactionFilterDto {

    public String fromDate;
    public String toDate;
    public String plateNumber;
    public Integer amount;
}

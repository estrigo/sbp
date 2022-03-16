package kz.spt.billingplugin.dto;

import kz.spt.billingplugin.model.Transaction;

import java.math.BigDecimal;
import java.util.Date;

public class TransactionDto {

    public String date;
    public String plateNumber;
    public BigDecimal amount;
    public String period;
    public String description;

    public static TransactionDto fromTransaction(Transaction transaction){
        TransactionDto transactionDto = new TransactionDto();
        transactionDto.amount = transaction.getAmount();
        transactionDto.description = transaction.getDescriptionRu();
        transactionDto.plateNumber = transaction.getPlateNumber();
        return transactionDto;
    }
}

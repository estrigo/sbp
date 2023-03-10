package kz.spt.billingplugin.dto;

import kz.spt.billingplugin.model.Transaction;

import java.math.BigDecimal;
import java.util.Date;

public class TransactionDto {

    public Long id;
    public String date;
    public String plateNumber;
    public BigDecimal amount;
    public String period;
    public String description;
    public String provider;
    public BigDecimal remainder;

    public static TransactionDto fromTransaction(Transaction transaction){
        TransactionDto transactionDto = new TransactionDto();
        transactionDto.id = transaction.getId();
        transactionDto.amount = transaction.getAmount();
        transactionDto.description = transaction.getDescriptionLocal();
        transactionDto.plateNumber = transaction.getPlateNumber();
        transactionDto.provider = transaction.getProvider();
        transactionDto.remainder = transaction.getRemainder();
        return transactionDto;
    }
}

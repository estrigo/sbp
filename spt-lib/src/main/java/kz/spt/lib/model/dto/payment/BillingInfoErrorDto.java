package kz.spt.lib.model.dto.payment;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;

public class BillingInfoErrorDto {

    public String txn_id;              // уникальны номер транзакции бил-ой системы Halyk
    public int result;                 // - 1 Показатель ошибки.
    public BigDecimal sum;                    // 200 - Сумма к оплате.
    public String message;             // "Некорректный номер авто свяжитесь с оператором."
    public BigDecimal current_balance =  BigDecimal.ZERO;

    public String currency;
}

package kz.spt.lib.model.dto.payment;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;

public class BillingPaymentSuccessDto {
    public String txn_id;              // уникальны номер транзакции бил-ой системы
    public int result;                 // 0 - Показатель успешно завершённой операции.
    public BigDecimal sum;             // 200 - Сумма к оплате.
    public String payment_id;          // 1-  уникальный ID транзакции на паркинге. Для сохранения в бил-ой системы Halyk.
}

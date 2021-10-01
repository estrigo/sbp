package kz.spt.lib.model.dto.payment;

public class BillingPaymentSuccessDto {
    public String txn_id;              // уникальны номер транзакции бил-ой системы Halyk
    public int result;                 // 0 - Показатель успешно завершённой операции.
    public int sum;                    // 200 - Сумма к оплате.
    public String payment_id;          // 1-  уникальный ID транзакции на паркинге. Для сохранения в бил-ой системы Halyk.
}

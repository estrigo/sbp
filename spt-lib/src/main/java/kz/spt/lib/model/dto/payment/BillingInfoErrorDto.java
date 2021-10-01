package kz.spt.lib.model.dto.payment;

public class BillingInfoErrorDto {

    public String txn_id;              // уникальны номер транзакции бил-ой системы Halyk
    public int result;                 // - 1 Показатель ошибки.
    public int sum;                    // 200 - Сумма к оплате.
    public String message;             // "Некорректный номер авто свяжитесь с оператором."
}

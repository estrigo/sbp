package kz.spt.lib.model.dto.payment;

public class BillingInfoSuccessDto {

    public String txn_id;              // уникальны номер транзакции бил-ой системы Halyk
    public int result;                 // 0 успешно завершено.
    public int current_balance;        // 0 текущий баланс клиента
    public int sum;                    // 200 - Сумма к оплате.
    public int left_free_time_minutes; // Кол-во оставшихся 15 минут на выезд.
    public String tariff;              // 100 тг/ч - Текстовое представление текущего тарифа.
    public String in_date;             // 2019-05-06 08:30:12 - Дата время последнего ВЪЕЗДА на паркинг.
    public int credit_amount;          // 0 - Сумма долга.
    /*Список записей о ВЪЕЗДЕ (когда клиент платит за 2 и более стоянок):
    "payment_details":
            [
    {
        "billing_log_id": "65",
            "amount": 56200, //- сумма за стоянку
            "type": "default"
    } ]*/
}

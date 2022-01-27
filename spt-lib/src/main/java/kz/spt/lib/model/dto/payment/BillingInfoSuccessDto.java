package kz.spt.lib.model.dto.payment;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;

public class BillingInfoSuccessDto {

    public String txn_id;              // уникальны номер транзакции бил-ой системы
    public int result;                 // 0 успешно завершено.
    public BigDecimal current_balance; // 0 текущий баланс клиента
    public BigDecimal sum;             // 200 - Сумма к оплате.
    public int left_free_time_minutes; // Кол-во оставшихся 15 минут на выезд.
    public String tariff;              // 100 тг/ч - Текстовое представление текущего тарифа.
    public String in_date;             // 2019-05-06 08:30:12 - Дата время последнего ВЪЕЗДА на паркинг.
    public int hours;
    /*Список записей о ВЪЕЗДЕ (когда клиент платит за 2 и более стоянок):
    "payment_details":
            [
    {
        "billing_log_id": "65",
            "amount": 56200, //- сумма за стоянку
            "type": "default"
    } ]*/
}

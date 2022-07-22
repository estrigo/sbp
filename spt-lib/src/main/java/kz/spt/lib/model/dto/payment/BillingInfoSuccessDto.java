package kz.spt.lib.model.dto.payment;

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

    public String currency;
}

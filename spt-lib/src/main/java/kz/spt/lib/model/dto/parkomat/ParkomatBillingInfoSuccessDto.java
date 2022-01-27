package kz.spt.lib.model.dto.parkomat;


import kz.spt.lib.model.dto.payment.BillingInfoSuccessDto;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ParkomatBillingInfoSuccessDto {
    String txn_id;              // уникальны номер транзакции бил-ой системы
    int result;                 // 0 успешно завершено.
    BigDecimal current_balance; // 0 текущий баланс клиента
    BigDecimal sum;             // 200 - Сумма к оплате.
    int left_free_time_minutes; // Кол-во оставшихся 15 минут на выезд.
    String tariff;              // 100 тг/ч - Текстовое представление текущего тарифа.
    String in_date;             // 2019-05-06 08:30:12 - Дата время последнего ВЪЕЗДА на паркинг.
    BigDecimal onlineSum;
    String payed_till;
    int hours;


    public static ParkomatBillingInfoSuccessDto convert(BillingInfoSuccessDto billingPaymentSuccessDto) {
        ParkomatBillingInfoSuccessDto dto = new ParkomatBillingInfoSuccessDto();
        dto.setTxn_id(billingPaymentSuccessDto.txn_id);
        dto.setCurrent_balance(billingPaymentSuccessDto.current_balance);
        dto.setResult(billingPaymentSuccessDto.result);
        dto.setLeft_free_time_minutes(billingPaymentSuccessDto.left_free_time_minutes);
        dto.setTariff(billingPaymentSuccessDto.tariff);
        dto.setIn_date(billingPaymentSuccessDto.in_date);
        dto.setSum(billingPaymentSuccessDto.sum);
        return dto;
    }
}

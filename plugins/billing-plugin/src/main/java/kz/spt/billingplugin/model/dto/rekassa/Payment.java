package kz.spt.billingplugin.model.dto.rekassa;
public class Payment{
    public PaymentType type = PaymentType.PAYMENT_CASH;
    public BillsCoins sum;

    public enum PaymentType {
        PAYMENT_CASH,
        PAYMENT_CARD
    }
}

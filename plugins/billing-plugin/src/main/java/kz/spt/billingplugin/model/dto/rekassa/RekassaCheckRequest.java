package kz.spt.billingplugin.model.dto.rekassa;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.checkerframework.checker.units.qual.A;


import java.util.ArrayList;

public class RekassaCheckRequest {
    public String operation = "OPERATION_SELL";
    public DateTime dateTime;
    public Operator operator = new Operator();
    public Domain domain = new Domain();
    @JsonProperty("items")
    public ArrayList<Item> items = new ArrayList<>();
    @JsonProperty("payments")
    public ArrayList<Payment> payments = new ArrayList<>();
    @JsonProperty("amounts")
    public Amounts amounts = new Amounts();

    public RekassaCheckRequest() {
        DateTime dateTime = new DateTime();
        dateTime.date = new Date();
        dateTime.time = new Time();
        this.dateTime = dateTime;

    }



    public void fillPayment(int amount, int change, boolean isIKKM) {
        Commodity commodity = new Commodity();
        commodity.price = new BillsCoins();
        commodity.price.bills = amount - change;

        commodity.sum = new BillsCoins();
        commodity.sum.bills = amount - change;

        commodity.auxiliary.add(new Auxiliary());

        //calc taxes
        Tax tax = new Tax();
        tax.sum = new BillsCoins();
        tax.sum.bills = (int)Math.round( (((double)amount -(double)change)/112*12)*100)/100;
        tax.sum.coins = (int)Math.round( (((double)amount -(double)change)/112*12)*100)%100;

        commodity.taxes.add(tax);

        Item item = new Item();
        item.commodity = commodity;



        this.items.add(item);


        this.amounts.total = new BillsCoins();
        this.amounts.total.bills =  amount - change;

        this.amounts.taken = new BillsCoins();
        this.amounts.taken.bills =  amount;

        this.amounts.change = new BillsCoins();
        this.amounts.change.bills = change;

        kz.spt.billingplugin.model.dto.rekassa.Payment payment = new kz.spt.billingplugin.model.dto.rekassa.Payment();
        payment.type = isIKKM ? kz.spt.billingplugin.model.dto.rekassa.Payment.PaymentType.PAYMENT_CARD : kz.spt.billingplugin.model.dto.rekassa.Payment.PaymentType.PAYMENT_CASH;
        payment.sum  = new BillsCoins();
        payment.sum.bills = amount - change;



        this.payments.add(payment);


    }
}

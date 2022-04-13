package kz.spt.billingplugin.model.dto.rekassa;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;

public class Commodity{
    public String name = "Оплата парковки";
    public String sectionCode = "1";
    public int quantity = 1000;
    public BillsCoins price;
    public BillsCoins sum;
    @JsonProperty("taxes")
    public ArrayList<Tax> taxes = new ArrayList<>();
    public ArrayList<Auxiliary> auxiliary = new ArrayList<>();
}

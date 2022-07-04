package kz.spt.lib.model;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.lib.extension.PluginRegister;
import kz.spt.lib.service.PluginService;
import kz.spt.lib.utils.StaticValues;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Audited
@Table(name = "car_state", indexes = {
        @Index(name = "car_number_idx", columnList = "car_number"),
        @Index(name = "out_timestamp_idx", columnList = "out_timestamp")
})
public class CarState {

    public enum CarOutType {
        REGISTER_PASS, // Регистрация выезда
        BOOKING_PASS, //Выезд через букинг
        DEBT_OUT, //Выезд с долгом
        FIFTEEN_FREE, // Выезд по бесплатным минутам
        ABONEMENT_PASS, // Выезд по абонементу
        WHITELIST_OUT, // Выезд по белому списку
        PAID_PASS, // платный выезд
        FREE_PASS // бесплатный выезд
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "car_number")
    private String carNumber;

    @Column(name = "in_timestamp")
    private Date inTimestamp;

    @Column(name = "out_timestamp")
    private Date outTimestamp;

    @Column(name = "in_photo_url")
    private String inPhotoUrl;

    @Column(name = "out_photo_url")
    private String outPhotoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private Parking.ParkingType type;

    @Column ( name="rate_amount", precision = 8, scale = 2 )
    private BigDecimal rateAmount;

    @Column ( name="amount", precision = 8, scale = 2 )
    private BigDecimal amount;

    private Boolean paid = false; // Заезжает на платной основе?

    private Long paymentId;

    private String inChannelIp;

    private String outChannelIp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parking")
    private Parking parking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "in_gate")
    private Gate inGate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "in_barrier")
    private Barrier inBarrier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "out_gate")
    private Gate outGate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "out_barrier")
    private Barrier outBarrier;

    @Column(name = "payment_json", columnDefinition = "text")
    private String paymentJson;

    @Column(name = "whitelist_json", columnDefinition = "text")
    private String whitelistJson;

    @Column(name = "booking_json", columnDefinition = "text")
    private String bookingJson;

    @Column(name = "zerotouch_json", columnDefinition = "text")
    private String zerotouchJson;

    @Column(name = "abonoment_json", columnDefinition = "text")
    private String abonomentJson;

    private Boolean cashlessPayment = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "car_out_type")
    private CarOutType carOutType;

    @CreationTimestamp
    private Date created;

    @UpdateTimestamp
    private Date updated;

    private Boolean isEnoughBalanceToLeave(PluginService pluginService, Boolean cashlessPayment) throws Exception {
        if(this.getPaid()){
            if(pluginService != null){
                PluginRegister ratePluginRegister = pluginService.getPluginRegister(StaticValues.ratePlugin);
                if (ratePluginRegister != null) {
                    SimpleDateFormat format = new SimpleDateFormat(StaticValues.dateFormatTZ);

                    ObjectMapper objectMapper = new ObjectMapper();
                    ObjectNode ratePluginNode = objectMapper.createObjectNode();
                    ratePluginNode.put("parkingId", this.getParking().getId());
                    ratePluginNode.put("inDate", format.format(this.getInTimestamp()));
                    ratePluginNode.put("outDate", format.format(this.getOutTimestamp() != null ? this.outTimestamp : new Date()));
                    ratePluginNode.put("cashlessPayment", cashlessPayment);
                    ratePluginNode.put("isCheck", false);
                    ratePluginNode.put("paymentsJson", this.getPaymentJson());

                    JsonNode ratePluginResult = ratePluginRegister.execute(ratePluginNode);
                    BigDecimal rateResult = ratePluginResult.get("rateResult").decimalValue().setScale(2);

                    PluginRegister billingPluginRegister = pluginService.getPluginRegister(StaticValues.billingPlugin);
                    if (billingPluginRegister != null) {
                        ObjectNode billinNode = objectMapper.createObjectNode();
                        billinNode.put("command", "getCurrentBalance");
                        billinNode.put("plateNumber", this.getCarNumber());
                        JsonNode billingResult = billingPluginRegister.execute(billinNode);
                        BigDecimal balance = billingResult.get("currentBalance").decimalValue().setScale(2);

                        return balance.compareTo(rateResult) >= 0;
                    } else {
                        throw new RuntimeException("billingPluginRegister is null");
                    }
                } else {
                    throw new RuntimeException("ratePluginRegister is null");
                }
            } else {
                throw new RuntimeException("pluginService is null");
            }
        } else {
            return true;
        }
    }

    private Boolean isCarLeft(){
        return this.outTimestamp != null;
    }
}

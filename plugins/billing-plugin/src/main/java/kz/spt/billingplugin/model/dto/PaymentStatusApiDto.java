package kz.spt.billingplugin.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentStatusApiDto {

    private byte status;

    private String providerTrnId;

    private byte transactionState;

    private byte transactionStateErrorStatus;

    private String transactionStateErrorMsg;
}

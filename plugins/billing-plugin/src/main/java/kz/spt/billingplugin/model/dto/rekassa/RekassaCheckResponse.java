package kz.spt.billingplugin.model.dto.rekassa;

import lombok.Data;

@Data
public class RekassaCheckResponse {
    String ticketNumber;
    String qrCode;
}

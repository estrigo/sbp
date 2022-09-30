package kz.spt.lib.model.dto;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CarStateFilterDto {

    private String dateFromString;
    private String dateToString;
    private String plateNumber;
    private Double amount;
    private Long inGateId;
    private Long outGateId;
    private boolean inParking;
}

package kz.spt.lib.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@AllArgsConstructor
public class CarStateFilterDto {

    private String dateFromString;
    private String dateToString;
    private String plateNumber;
    private Integer amount;
    private Long inGateId;
    private Long outGateId;
    private boolean inParking;
}

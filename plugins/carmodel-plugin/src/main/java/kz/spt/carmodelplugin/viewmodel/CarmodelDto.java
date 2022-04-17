package kz.spt.carmodelplugin.viewmodel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class CarmodelDto {

    private String dateFromString;
    private String dateToString;
    private String entryDate;
    private String plateNumber;
    private Long inGateId;
    private String dimension;
    private String carModel;
    private String photo;
    private String bigPhoto;

    public CarmodelDto() {

    }
}

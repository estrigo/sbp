package kz.spt.lib.model.dto.parkomat;

import kz.spt.lib.model.CarState;
import lombok.Data;

@Data
public class InCarResponseDTO {
    String id;
    String car_number;

    public static InCarResponseDTO info(CarState carState) {
        InCarResponseDTO dto = new InCarResponseDTO();
        dto.setCar_number(carState.getCarNumber());
        dto.setId(carState.getId().toString());
        return dto;
    }
}

package kz.spt.lib.model.dto.parkomat;

import lombok.Data;

/**
 *
 */
@Data

public class CarInRequestDTO {
    private String where;
    private String username;
    private String car_number;
}

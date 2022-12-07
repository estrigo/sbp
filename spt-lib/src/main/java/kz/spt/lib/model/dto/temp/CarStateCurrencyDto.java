package kz.spt.lib.model.dto.temp;

import kz.spt.lib.model.dto.CarStateDto;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CarStateCurrencyDto {
    private CarStateDto carState;
    private String currency;
}

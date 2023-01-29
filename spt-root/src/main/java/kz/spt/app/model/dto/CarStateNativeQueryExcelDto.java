package kz.spt.app.model.dto;

import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Accessors(chain = true)
public class CarStateNativeQueryExcelDto {
    String carNumber;
    Date inTimestamp;
    Date outTimestamp;
    String inGate;
    String outGate;
    String status;
    String duration;
}

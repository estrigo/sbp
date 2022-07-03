package kz.spt.prkstatusplugin.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ParkomatUpdateDTO {
    long id;
    String date;
    String desc;
    String file;
    String type;
}

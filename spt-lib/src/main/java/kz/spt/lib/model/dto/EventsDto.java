package kz.spt.lib.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
public class EventsDto {
    private Long id;
    private Date created;
    private String plateNumber;
    private String description;
    private String eventType;
    private String imgUrl;
}

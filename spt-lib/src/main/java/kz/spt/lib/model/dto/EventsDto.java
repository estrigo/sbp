package kz.spt.lib.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
public class EventsDto {
    private Long id;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = JsonFormat.DEFAULT_TIMEZONE)
    private Date created;

    private String plateNumber;
    private String description;
    private String descriptionEn;
    private String descriptionDe;
    private String eventType;
    private String gate;
    private String smallImgUrl;
    private String bigImgUrl;
}

package kz.spt.app.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@NoArgsConstructor
@Data
public class Period {
    private Date start;
    private Date end;
}

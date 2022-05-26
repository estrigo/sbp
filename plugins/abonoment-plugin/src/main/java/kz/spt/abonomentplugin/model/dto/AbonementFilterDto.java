package kz.spt.abonomentplugin.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AbonementFilterDto {

    private String dateFromString;
    private String dateToString;
    private String carNumber;
    private String searchAbonementTypes;
}

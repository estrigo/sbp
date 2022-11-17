
package kz.spt.rateplugin.model.dto;

import kz.spt.lib.model.Dimensions;
import kz.spt.rateplugin.model.IntervalRate;
import kz.spt.rateplugin.model.ParkingRate;
import kz.spt.rateplugin.model.RateCondition;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntervalRateDto {

    private IntervalRate intervalRate;
    private String datetimeFrom;
    private String datetimeTo;
    private List<RateCondition> rateConditions;
    private ParkingRate parkingRate;
    private Dimensions dimensionSet;
}

package kz.spt.rateplugin.model.dto;

import kz.spt.lib.model.Dimensions;
import kz.spt.rateplugin.model.ParkingRate;
import kz.spt.rateplugin.model.RateCondition;
import lombok.Data;

import javax.persistence.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Data
public class IntervalRateDto {

    private Long id;
    private String datetimeFrom;
    private String datetimeTo;
    private List<RateCondition> rateConditions;
    private ParkingRate parkingRate;
    private Set<Dimensions> dimensionSet;
}

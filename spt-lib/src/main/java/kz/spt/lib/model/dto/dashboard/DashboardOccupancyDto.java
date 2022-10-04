package kz.spt.lib.model.dto.dashboard;

import kz.spt.lib.service.DashboardService;
import lombok.Data;

@Data
public class DashboardOccupancyDto {

    private Long total = 0L;
    private Long occupied = 0L;
    private Long percentage = 0L;

    public DashboardOccupancyDto(Long total, Long occupied, Long percentage){
        this.total = total;
        this.occupied = occupied;
        this.percentage = percentage;
    }
}

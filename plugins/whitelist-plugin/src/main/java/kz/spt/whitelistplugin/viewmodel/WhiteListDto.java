package kz.spt.whitelistplugin.viewmodel;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WhiteListDto {
    private long id;
    private String plateNumber;
    private String parkingName;
    private String groupName;
    private String conditionDetail;
}
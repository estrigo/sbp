package kz.spt.whitelistplugin.viewmodel;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class WhiteListGroupDto {
    private long id;
    private String name;
    private String size;
    private String parkingName;
    private String conditionDetail;
    private String whiteLists;
}

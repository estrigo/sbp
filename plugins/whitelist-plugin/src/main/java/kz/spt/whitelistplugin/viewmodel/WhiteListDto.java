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
    private String createdUser;
    private String createdDate;
    private String fullName;
    private String address;
    private String parkingNumber;
    private String comment;
    private String updatedDate;
    private String updatedUser;
}

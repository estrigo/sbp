package kz.spt.lib.model.dto.adminPlace;

import kz.spt.lib.model.dto.adminPlace.enums.AdminRequestResponseType;
import lombok.Data;

import java.util.List;

@Data
public class AdminCommandDto {
    private String uid;
    private AdminRequestResponseType type;
    private List<Prop> props;
    private Object object;
    private Object gitInfo;


    public AdminCommandDto() {
    }

    public AdminCommandDto(Object object) {
        this.object = object;
    }

    public AdminCommandDto(Object object, Object gitInfo, String uid) {
        this.object = object;
        this.gitInfo = gitInfo;
        this.uid = uid;
    }
}

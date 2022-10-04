package kz.spt.app.model.dto;

import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;

@Data
@Builder
public class GitInfoDto {
    private String branch;
    private String buildHost;
    private String buildTime;
    private String buildUserName;
    private String buildVersion;
    private String closestTagName;
    private String tags;
    private String commitId;
    private String commitMessageShort;
    private String commitUserName;

}

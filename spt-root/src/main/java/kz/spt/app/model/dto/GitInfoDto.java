package kz.spt.app.model.dto;

import lombok.Builder;
import lombok.Data;

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

package kz.spt.app.service.impl;

import kz.spt.app.model.dto.GitInfoDto;
import kz.spt.lib.service.GitInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@PropertySource("classpath:git.properties")
public class GitInfoServiceImpl implements GitInfoService {


    @Value("${git.branch}")
    private String branch;

    @Value("${git.build.host}")
    private String buildHost;

    @Value("${git.build.time}")
    private String buildTime;

    @Value("${git.build.user.name}")
    private String buildUserName;

    @Value("${git.build.version}")
    private String buildVersion;

    @Value("${git.closest.tag.name}")
    private String closestTagName;

    @Value("${git.tags}")
    private String tags;
    @Value("${git.commit.id}")
    private String commitId;

    @Value("${git.commit.message.short}")
    private String commitMessageShort;

    @Value("${git.commit.user.name}")
    private String commitUserName;

    public GitInfoDto getGitInfo() {
        return GitInfoDto.builder()
                .branch(branch)
                .buildHost(buildHost)
                .buildTime(buildTime)
                .buildUserName(buildUserName)
                .buildVersion(buildVersion)
                .closestTagName(closestTagName)
                .tags(tags)
                .commitId(commitId)
                .commitMessageShort(commitMessageShort)
                .commitUserName(commitUserName)
                .build();
    }


    @Override
    public Object gitInfo() {
        return getGitInfo();
    }
}

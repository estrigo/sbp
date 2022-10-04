package kz.spt.app.model.dto;

import lombok.Data;

import java.util.Set;


@Data
public class WhlResponse {

    private Set<String> plateNumbers;
    private Long groupId;
}

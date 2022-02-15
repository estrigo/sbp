package kz.spt.lib.model.dto;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
public class SendMailDto {
    private String subject;
    private String message;
}

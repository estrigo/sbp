package kz.spt.lib.model.dto;

import kz.spt.lib.model.Blacklist;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class BlacklistDto {
    private Long id;
    private String plateNumber;
    private Blacklist.BlacklistType type;
}

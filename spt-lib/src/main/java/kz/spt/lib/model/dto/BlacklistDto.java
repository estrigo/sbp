package kz.spt.lib.model.dto;

import kz.spt.lib.model.Blacklist;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BlacklistDto {

    private Long id;
    private String plateNumber;
    private String type;

    public static BlacklistDto fromBlacklist(Blacklist blacklist){
        BlacklistDto dto = new BlacklistDto();
        dto.setId(blacklist.getId());
        dto.setPlateNumber(blacklist.getPlateNumber());
        dto.setType(blacklist.getType());

        return dto;
    }

    public static List<BlacklistDto> fromBlacklists(List<Blacklist> blacklists){
        List<BlacklistDto> dtoList = new ArrayList<>();
        for (Blacklist blacklist : blacklists) {
            dtoList.add(fromBlacklist(blacklist));
        }
        return dtoList;
    }
}

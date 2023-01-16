package kz.spt.abonomentplugin.dto;

import kz.spt.abonomentplugin.model.AbonomentTypes;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class AbonomentTypeDTO {

	public Long id;
	public int period;
	public int price;
	public String type;
	public String custom;
	public String createdUser;

	public static AbonomentTypeDTO convertToDto(AbonomentTypes abonomentType) {
		AbonomentTypeDTO dto = new AbonomentTypeDTO();
		dto.period = abonomentType.getPeriod();
		dto.price = abonomentType.getPrice();
		dto.id = abonomentType.getId();
		dto.type = abonomentType.getType();
		dto.custom = abonomentType.getCustomJson();
		dto.createdUser = abonomentType.getCreatedUser();
		return dto;
	}

	public static List<AbonomentTypeDTO> convertToDto(List<AbonomentTypes> abonomentTypes) {
		List<AbonomentTypeDTO> dtoList = new ArrayList<>(abonomentTypes.size());
		for (AbonomentTypes abonomentType : abonomentTypes) {
			dtoList.add(convertToDto(abonomentType));
		}
		return dtoList;
	}
}

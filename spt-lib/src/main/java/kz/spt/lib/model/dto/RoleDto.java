package kz.spt.lib.model.dto;

import kz.spt.lib.model.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoleDto {
    private int id;
    private String name;
    private String name_ru;
    private String name_en;
    private String name_local;
    private String plugin;


    public static List<RoleDto> roleDtos(List<Role> roles){
        List<RoleDto> roleDtos = new ArrayList<>();
        for(Role role: roles){
            RoleDto roleDto = new RoleDto();
            roleDto.id = role.getId();
            roleDto.name = role.getName();
            roleDto.name_ru = role.getName_ru();
            roleDto.name_en = role.getName_en();
            roleDto.plugin = role.getPlugin();
            roleDtos.add(roleDto);
        }
        return roleDtos;
    }

//    public String setNameDe(String nameEn){
//        Map<String, String> roleNames = new HashMap<>();
//        roleNames.put("Administrator", "");
//        roleNames.put("User", "Benutzer");
//        roleNames.put("Manager", "Administrator");
//        roleNames.put("Owner", "Administrator");
//        roleNames.put("Test user", "Administrator");
//        roleNames.put("Operator", "Administrator");
//        roleNames.put("Operator NO REVENUE SHARE", "Administrator");
//        roleNames.put("read", "Administrator");
//        roleNames.put("Accountant", "Administrator");
//        roleNames.put("Operator Parqour", "Administrator");
//    }
}

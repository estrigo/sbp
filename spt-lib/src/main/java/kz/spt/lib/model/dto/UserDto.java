package kz.spt.lib.model.dto;

import kz.spt.lib.model.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String patronymic;
    private String password;
    private int enabled;
    private List<RoleDto> roles;

    public static UserDto userToUserDto(User user){
        UserDto userDto = new UserDto();
        userDto.id = user.getId();
        userDto.username = user.getUsername();
        userDto.email = user.getEmail();
        userDto.firstName = user.getFirstName();
        userDto.lastName = user.getLastName();
        userDto.patronymic = user.getPatronymic();
        userDto.password = user.getPassword();
        userDto.enabled = user.getEnabled();
        userDto.roles = RoleDto.roleDtos(user.getRoles());
        return userDto;
    }

}

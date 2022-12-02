package kz.spt.lib.service;

import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.lib.model.User;
import kz.spt.lib.model.dto.UserDto;

public interface UserService {

    User findByUsername(String username);

    Iterable<User> listAllUsers();

    User showUser(Long id);

    void saveUser(User user);

    void editUser(User user);

    void deleteUser(User user);

    Page<UserDto> getUsers(PagingRequest pagingRequest);




}

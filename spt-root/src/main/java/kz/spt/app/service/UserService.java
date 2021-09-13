package kz.spt.app.service;

import kz.spt.api.bootstrap.datatable.Page;
import kz.spt.api.bootstrap.datatable.PagingRequest;
import kz.spt.api.model.User;

public interface UserService {

    User findByUsername(String username);

    Iterable<User> listAllUsers();

    User showUser(Long id);

    void saveUser(User user);

    void editUser(User user);

    void deleteUser(User user);

    Page<User> getUsers(PagingRequest pagingRequest);




}

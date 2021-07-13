package kz.spt.app.service;

import kz.spt.app.entity.User;

public interface UserService {

    User findByUsername(String username);

    Iterable<User> listAllUsers();

    User showUser(Long id);

    void saveUser(User user);

    void editUser(User user);

    void deleteUser(User user);

}

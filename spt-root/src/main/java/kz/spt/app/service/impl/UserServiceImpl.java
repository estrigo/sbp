package kz.spt.app.service.impl;

import kz.spt.api.model.Role;
import kz.spt.api.model.User;
import kz.spt.app.repository.RoleRepository;
import kz.spt.app.repository.UserRepository;
import kz.spt.app.service.SpringDataUserDetailsService;
import kz.spt.app.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private BCryptPasswordEncoder passwordEncoder;
    private AuthenticationManager authenticationManager;
    private SpringDataUserDetailsService springDataUserDetailsService;

    @Autowired
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Autowired
    public void setRoleRepository(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Autowired
    public void setPasswordEncoder(BCryptPasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Autowired
    public void setAuthenticationManager(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Autowired
    public void setSpringDataUserDetailsService(SpringDataUserDetailsService springDataUserDetailsService) {
        this.springDataUserDetailsService = springDataUserDetailsService;
    }

    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Iterable<User> listAllUsers() {
        return userRepository.findAllByEnabled(1);
    }

    @Override
    public User showUser(Long id) {
        return userRepository.getOne(id);
    }

    @Override
    public void saveUser(User user) {
        Role userRole = roleRepository.findByName("ROLE_USER");
        List<Role> roles = new ArrayList<>();
        roles.add(userRole);
        user.setRoles(roles);
        user.setEnabled(1);
        String password = user.getPassword();
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
    }

    @Override
    public void editUser(User user) {
        String password = user.getPassword();
        user.setPassword(passwordEncoder.encode(password));
        Role userRole = roleRepository.findByName("ROLE_USER");
        List<Role> roles = new ArrayList<>();
        roles.add(userRole);
        try {
            for(Role role: user.getRoles()){
                userRole = roleRepository.getOne(role.getId());
                if(!roles.contains(userRole)){
                    roles.add(userRole);
                }
            }
        } catch (NullPointerException e) {
            userRole = roleRepository.findByName("ROLE_USER");
        } finally {
            user.setRoles(roles);
            user.setEnabled(1);
            userRepository.save(user);
        }
    }

    @Override
    public void deleteUser(User user) {
        user.setEnabled(0);
        user.setPassword(null);
        userRepository.save(user);
    }

}

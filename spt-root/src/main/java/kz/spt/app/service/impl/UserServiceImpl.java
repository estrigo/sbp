package kz.spt.app.service.impl;

import kz.spt.api.bootstrap.datatable.*;
import kz.spt.api.model.Cars;
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
import org.thymeleaf.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private BCryptPasswordEncoder passwordEncoder;
    private AuthenticationManager authenticationManager;
    private SpringDataUserDetailsService springDataUserDetailsService;

    private static final Comparator<User> EMPTY_COMPARATOR = (e1, e2) -> 0;

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
            for (Role role : user.getRoles()) {
                userRole = roleRepository.getOne(role.getId());
                if (!roles.contains(userRole)) {
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

    @Override
    public Page<User> getUsers(PagingRequest pagingRequest) {
        List<User> users = userRepository.findAll();
        return getPage(users, pagingRequest);

    }

    private Page<User> getPage(List<User> users, PagingRequest pagingRequest) {
        List<User> filtered = users.stream()
                .sorted(sortUsers(pagingRequest))
                .filter(filterUsers(pagingRequest))
                .skip(pagingRequest.getStart())
                .limit(pagingRequest.getLength())
                .collect(Collectors.toList());

        long count = users.stream()
                .filter(filterUsers(pagingRequest))
                .count();

        Page<User> page = new Page<>(filtered);
        page.setRecordsFiltered((int) count);
        page.setRecordsTotal((int) count);
        page.setDraw(pagingRequest.getDraw());

        return page;
    }

    private Predicate<User> filterUsers(PagingRequest pagingRequest) {
        if (pagingRequest.getSearch() == null || StringUtils.isEmpty(pagingRequest.getSearch()
                .getValue())) {
            return users -> true;
        }
        String value = pagingRequest.getSearch().getValue();

        return users -> (users.getUsername() != null && users.getUsername().toLowerCase().contains(value))
                || (users.getEmail() != null && users.getEmail().toLowerCase().contains(value))
                || (users.getFirstName() != null && users.getFirstName().toLowerCase().contains(value))
                || (users.getLastName() != null && users.getLastName().toLowerCase().contains(value));
    }

    private Comparator<User> sortUsers(PagingRequest pagingRequest) {
        if (pagingRequest.getOrder() == null) {
            return EMPTY_COMPARATOR;
        }

        try {
            Order order = pagingRequest.getOrder()
                    .get(0);

            int columnIndex = order.getColumn();
            Column column = pagingRequest.getColumns()
                    .get(columnIndex);

            Comparator<User> comparator = UsersComparator.getComparator(column.getData(), order.getDir());
            return Objects.requireNonNullElse(comparator, EMPTY_COMPARATOR);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return EMPTY_COMPARATOR;
    }


}

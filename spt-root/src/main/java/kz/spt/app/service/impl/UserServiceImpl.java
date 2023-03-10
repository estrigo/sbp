package kz.spt.app.service.impl;

import kz.spt.lib.model.dto.UserDto;
import kz.spt.lib.bootstrap.datatable.*;
import kz.spt.lib.model.Role;
import kz.spt.lib.model.User;
import kz.spt.app.repository.RoleRepository;
import kz.spt.app.repository.UserRepository;
import kz.spt.lib.service.LanguagePropertiesService;
import kz.spt.lib.service.UserService;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.util.StringUtils;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Log
@Service
@Transactional(noRollbackFor = Exception.class)
public class UserServiceImpl implements UserService {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private LanguagePropertiesService languagePropertiesService;
    private BCryptPasswordEncoder passwordEncoder;

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
    public void setLanguagePropertiesService(LanguagePropertiesService languagePropertiesService) {
        this.languagePropertiesService = languagePropertiesService;
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
        user.setEnabled(1);
        if(user.getId() != null){
            String oldPasswordHash = userRepository.getPasswordHashFromDb(user.getId());
            if(user.getPassword() !=null && !"".equals(user.getPassword()) && !oldPasswordHash.equals(user.getPassword())){
                String password = user.getPassword();
                user.setPassword(passwordEncoder.encode(password));
            } else {
                user.setPassword(oldPasswordHash);
            }
        } else {
            String password = user.getPassword();
            user.setPassword(passwordEncoder.encode(password));
        }
        userRepository.save(user);
    }

    @Override
    public void editUser(User user) {
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
            saveUser(user);
        }
    }

    @Override
    public void deleteUser(User user) {
        if(user.getRoles() != null){
            user.setRoles(null);
        }
        userRepository.delete(user);
    }

    @Override
    public Page<UserDto> getUsers(PagingRequest pagingRequest) {
        List<User> users = userRepository.findAll();
        return getPage(users, pagingRequest);
    }

    private Page<UserDto> getPage(List<User> users, PagingRequest pagingRequest) {
        List<User> filtered = users.stream()
                .sorted(sortUsers(pagingRequest))
                .filter(filterUsers(pagingRequest))
                .skip(pagingRequest.getStart())
                .limit(pagingRequest.getLength())
                .collect(Collectors.toList());

        long count = users.stream()
                .filter(filterUsers(pagingRequest))
                .count();

        List<UserDto> userDtos = getUserDtos(filtered);
        Page<UserDto> page = new Page<>(userDtos);
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

        return users -> (users.getUsername() != null && users.getUsername().toLowerCase().contains(value.toLowerCase()))
                || (users.getEmail() != null && users.getEmail().toLowerCase().contains(value.toLowerCase()))
                || (users.getFirstName() != null && users.getFirstName().toLowerCase().contains(value.toLowerCase()))
                || (users.getLastName() != null && users.getLastName().toLowerCase().contains(value.toLowerCase()));
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

    private List<UserDto> getUserDtos(List<User> users){
        List<UserDto> userDtos = new ArrayList<>();
        for (User user: users){
            UserDto userDto = UserDto.userToUserDto(user);
            userDto.getRoles()
                    .forEach(roleDto -> roleDto.setName_local(languagePropertiesService.getMessageFromProperties(roleDto.getName_en())));
            userDtos.add(userDto);
        }
        return userDtos;
    }

}

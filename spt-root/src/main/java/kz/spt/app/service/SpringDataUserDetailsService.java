package kz.spt.app.service;

import kz.spt.lib.model.CurrentUser;
import kz.spt.lib.model.Role;
import kz.spt.lib.model.User;
import kz.spt.lib.service.UserService;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Log
@Service
public class SpringDataUserDetailsService implements UserDetailsService {

    @Autowired
    UserService userService;

    @Autowired
    RoleService roleService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userService.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }

        Set<GrantedAuthority> grantedAuthorities = new HashSet<>();

        Boolean hasSuperAdmin = false;
        for(Role role:user.getRoles()){
            if("ROLE_SUPERADMIN".equals(role.getName())){
                hasSuperAdmin = true;
            }
        }
        if(hasSuperAdmin){
            for(Role role:roleService.listAllRoles()){
                grantedAuthorities.add(new SimpleGrantedAuthority(role.getName()));
            }
        } else {
            for(Role role:user.getRoles()){
                grantedAuthorities.add(new SimpleGrantedAuthority(role.getName()));
            }
        }

        CurrentUser currentUser = new CurrentUser();
        currentUser.setUser(user);
        currentUser.setAuthorities(grantedAuthorities);
        return currentUser;
    }

}

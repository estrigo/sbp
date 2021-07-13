package kz.spt.app.service;

import kz.spt.app.entity.Role;

public interface RoleService {

    Iterable<Role> listAllRoles();

    Iterable<Role> listRolesByPlugins();
}

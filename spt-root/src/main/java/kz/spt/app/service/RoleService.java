package kz.spt.app.service;

import kz.spt.lib.model.Role;

public interface RoleService {

    Iterable<Role> listAllRoles();

    Iterable<Role> listRolesByPlugins();
}

package kz.spt.app.service;

import kz.spt.api.model.Role;

public interface RoleService {

    Iterable<Role> listAllRoles();

    Iterable<Role> listRolesByPlugins();
}

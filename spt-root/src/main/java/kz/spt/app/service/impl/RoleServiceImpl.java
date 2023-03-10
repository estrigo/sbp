package kz.spt.app.service.impl;

import kz.spt.lib.model.Role;
import kz.spt.app.repository.RoleRepository;
import kz.spt.app.service.RoleService;
import org.pf4j.PluginManager;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(noRollbackFor = Exception.class)
public class RoleServiceImpl implements RoleService {

    private RoleRepository roleRepository;

    private PluginManager pluginManager;

    public RoleServiceImpl(RoleRepository roleRepository, PluginManager pluginManager) {
        this.roleRepository = roleRepository;
        this.pluginManager = pluginManager;
    }

    @Override
    public Iterable<Role> listAllRoles() {
        return roleRepository.findAll();
    }

    @Override
    public Iterable<Role> listRolesByPlugins() {
        List<String> pluginIds = new ArrayList<>();

        List<PluginWrapper> plugins = pluginManager.getPlugins();
        for(PluginWrapper plugin:plugins){
            if(plugin != null || PluginState.STARTED.equals(plugin.getPluginState())){
                pluginIds.add(plugin.getPluginId());
            }
        }

        if(pluginIds.size() == 0){
            return roleRepository.findAllByPluginIsNull();
        }

        return roleRepository.findAllByPluginIn(pluginIds);
    }

}

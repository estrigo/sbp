package crm.service;

import crm.entity.Role;
import crm.repository.RoleRepository;
import org.pf4j.PluginManager;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
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

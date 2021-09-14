package kz.spt.whitelistplugin.service;

import kz.spt.whitelistplugin.model.WhitelistGroups;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface WhitelistGroupsService {

    WhitelistGroups findById(Long id);

    public WhitelistGroups getWithCars(Long id);

    WhitelistGroups createGroup(String name, String[] carList, String username);

    void updateGroup(Long groupId, String name, String[] carList, String username);

    void deleteGroup(WhitelistGroups group);
}

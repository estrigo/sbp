package kz.spt.app.service.impl;


import kz.spt.lib.model.Groups;
import kz.spt.lib.service.GroupsService;
import kz.spt.app.repository.GroupsRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GroupsServiceImpl implements GroupsService {

    private GroupsRepository groupsRepository;

    public GroupsServiceImpl(GroupsRepository groupsRepository) {
        this.groupsRepository = groupsRepository;
    }


    @Override
    public Groups findById(Long id) {
        return groupsRepository.getOne(id);
    }

    @Override
    public List<Groups> listAllGroups() {
        return groupsRepository.findAll();
    }

    @Override
    public Groups saveGroups() {
        return null;
    }

    @Override
    public void createGroup(String name) {
    }
}

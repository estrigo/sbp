package kz.spt.lib.service;

import kz.spt.lib.model.Groups;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface GroupsService {

    Groups findById(Long id);

    List<Groups> listAllGroups();

    Groups saveGroups();

    void createGroup(String name);




}

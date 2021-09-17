package kz.spt.whitelistplugin.service;

import kz.spt.whitelistplugin.model.Whitelist;
import kz.spt.whitelistplugin.model.WhitelistGroups;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.List;

@Service
public interface WhitelistGroupsService {

    WhitelistGroups findById(Long id);

    WhitelistGroups getWithCars(Long id);

    WhitelistGroups saveWhitelistGroup(WhitelistGroups whitelistGroups, String currentUser) throws ParseException;

    void deleteGroup(WhitelistGroups group);

    Iterable<WhitelistGroups> listAllWhitelistGroups();

    void deleteById(Long id);

    WhitelistGroups prepareById(Long id);
}

package kz.spt.whitelistplugin.service;

import com.fasterxml.jackson.databind.node.ArrayNode;
import kz.spt.whitelistplugin.model.Whitelist;
import kz.spt.whitelistplugin.model.WhitelistGroups;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Date;
import java.util.List;

public interface WhitelistService {

    void saveWhitelist(Whitelist whitelist, UserDetails currentUser) throws Exception;

    Iterable<Whitelist> listAllWhitelist();

    List<Whitelist> listByGroupId(Long groupId);

    ArrayNode hasAccess(String plateNumber, Date enterDate);

    Whitelist prepareById(Long id);

    void deleteById(Long id);

    void saveWhitelistFromGroup(String plateNumber, WhitelistGroups group, String currentUser);
}
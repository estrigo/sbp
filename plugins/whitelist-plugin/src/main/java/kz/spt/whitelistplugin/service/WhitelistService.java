package kz.spt.whitelistplugin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import kz.spt.whitelistplugin.model.Whitelist;
import kz.spt.whitelistplugin.model.WhitelistGroups;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Date;
import java.util.List;

public interface WhitelistService {

    void saveWhitelist(Whitelist whitelist, UserDetails currentUser) throws Exception;

    Iterable<Whitelist> listAllWhitelist() throws JsonProcessingException;

    List<Whitelist> listByGroupId(Long groupId);

    ArrayNode hasAccess(Long parkingId, String plateNumber, Date enterDate) throws JsonProcessingException;

    Whitelist prepareById(Long id);

    void deleteById(Long id);

    void saveWhitelistFromGroup(String plateNumber, WhitelistGroups group, String currentUser);
}
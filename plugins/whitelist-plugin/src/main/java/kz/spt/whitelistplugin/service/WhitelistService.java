package kz.spt.whitelistplugin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.lib.model.Parking;
import kz.spt.whitelistplugin.model.Whitelist;
import kz.spt.whitelistplugin.model.WhitelistGroups;
import kz.spt.whitelistplugin.viewmodel.WhiteListDto;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface WhitelistService {

    void saveWhitelist(Whitelist whitelist, UserDetails currentUser) throws Exception;

    Iterable<Whitelist> listAllWhitelist() throws JsonProcessingException;

    Page<WhiteListDto> listByPage(PagingRequest pagingRequest);

    List<Whitelist> listByGroupId(Long groupId);

    List<Whitelist> whitelistByGroupName(String groupName) throws JsonProcessingException;

    ArrayNode hasAccess(Long parkingId, String plateNumber, Date enterDate) throws JsonProcessingException;

    Whitelist prepareById(Long id);

    void deleteById(Long id);

    void saveWhitelistFromGroup(String plateNumber, WhitelistGroups group, String currentUser, Parking parking) throws Exception;

    ArrayNode getList(Long parkingId, String plateNumber) throws JsonProcessingException;

    Whitelist findByPlatenumber(String platenumber, Long parkingId);

    List<String> getExistingPlatenumbers(List<String> platenumbers, Long parkingId);

    List<String> getExistingPlatenumbers(List<String> platenumbers, Long parkingId, Long groupId);

    List<WhiteListDto> listAllWhitelistForExcel() throws JsonProcessingException;

    List<WhiteListDto> listByGroupName(String groupName) throws JsonProcessingException;

    void deleteAllByParkingId(Long parkingId);

    ArrayNode getValidWhiteListsInPeriod(Long parkingId, String platenumber, Date inDate, Date outDate) throws JsonProcessingException;

    List<String> findCarsByPlatenumber(String platenumber);

    List<String> findPlatenumbersByPlatenumber(String platenumber);

    Optional<WhitelistGroups> findWhitelistGroups(Long groupId);
}
package kz.spt.whitelistplugin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.whitelistplugin.model.Whitelist;
import kz.spt.whitelistplugin.model.WhitelistGroups;
import kz.spt.whitelistplugin.viewmodel.WhiteListDto;
import kz.spt.whitelistplugin.viewmodel.WhiteListGroupDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.util.List;

@Service
@Transactional(noRollbackFor = Exception.class)
public interface WhitelistGroupsService {


    WhitelistGroups getWithCars(Long id);

    WhitelistGroups saveWhitelistGroup(WhitelistGroups whitelistGroups, String currentUser) throws Exception;

    void deleteGroup(WhitelistGroups group);

    Iterable<WhitelistGroups> listAllWhitelistGroups() throws JsonProcessingException;

    Page<WhiteListGroupDto> listByPage(PagingRequest pagingRequest);

    void deleteById(Long id);

    WhitelistGroups prepareById(Long id);

    List<WhiteListGroupDto> listByParkingId(Long parkingId);
}

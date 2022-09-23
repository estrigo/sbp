package kz.spt.whitelistplugin.service.impl;


import com.fasterxml.jackson.core.JsonProcessingException;
import kz.spt.lib.bootstrap.datatable.Column;
import kz.spt.lib.bootstrap.datatable.Order;
import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.lib.model.Parking;
import kz.spt.whitelistplugin.bootstrap.datatable.WhiteListComparators;
import kz.spt.whitelistplugin.bootstrap.datatable.WhiteListGroupComparators;
import kz.spt.whitelistplugin.model.Whitelist;
import kz.spt.whitelistplugin.model.WhitelistGroups;
import kz.spt.whitelistplugin.repository.WhitelistGroupsRepository;
import kz.spt.whitelistplugin.repository.WhitelistRepository;
import kz.spt.whitelistplugin.service.RootServicesGetterService;
import kz.spt.whitelistplugin.service.WhitelistGroupsService;
import kz.spt.whitelistplugin.service.WhitelistService;
import kz.spt.whitelistplugin.viewmodel.WhiteListDto;
import kz.spt.whitelistplugin.viewmodel.WhiteListGroupDto;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.pf4j.util.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@Log
@Transactional(noRollbackFor = Exception.class)
public class WhitelistGroupsServiceImpl implements WhitelistGroupsService {

    private final String dateformat = "yyyy-MM-dd'T'HH:mm";
    private WhitelistRepository whitelistRepository;
    private WhitelistGroupsRepository whitelistGroupsRepository;
    private RootServicesGetterService rootServicesGetterService;
    private WhitelistService whitelistService;
    private static final Comparator<WhiteListGroupDto> EMPTY_COMPARATOR = (e1, e2) -> 0;

    public WhitelistGroupsServiceImpl(WhitelistRepository whitelistRepository,
                                      WhitelistGroupsRepository whitelistGroupsRepository,
                                      RootServicesGetterService rootServicesGetterService,
                                      WhitelistService whitelistService) {
        this.whitelistRepository = whitelistRepository;
        this.whitelistGroupsRepository = whitelistGroupsRepository;
        this.rootServicesGetterService = rootServicesGetterService;
        this.whitelistService = whitelistService;
    }

    @Override
    public WhitelistGroups findById(Long id) {
        return whitelistGroupsRepository.getOne(id);
    }

    @Override
    public WhitelistGroups getWithCars(Long id) {
        return whitelistGroupsRepository.getWhitelistGroup(id);
    }

    @Override
    public WhitelistGroups saveWhitelistGroup(WhitelistGroups whitelistGroups, String currentUser) throws Exception {
        SimpleDateFormat format = new SimpleDateFormat(dateformat);
        if (Whitelist.Type.PERIOD.equals(whitelistGroups.getType())) {
            if (StringUtils.isNotNullOrEmpty(whitelistGroups.getAccessStartString())) {
                whitelistGroups.setAccess_start(format.parse(whitelistGroups.getAccessStartString()));
            }
            if (StringUtils.isNotNullOrEmpty(whitelistGroups.getAccessEndString())) {
                whitelistGroups.setAccess_end(format.parse(whitelistGroups.getAccessEndString()));
            }
        } else {
            if (!Whitelist.Type.CUSTOM.equals(whitelistGroups.getType())) {
                whitelistGroups.setCustomJson(null);
            }
            whitelistGroups.setAccess_start(null);
            whitelistGroups.setAccess_end(null);
        }
        Parking parking = rootServicesGetterService.getParkingService().findById(whitelistGroups.getParkingId());
        whitelistGroups.setParking(parking);
        whitelistGroups.setUpdatedUser(currentUser);
        WhitelistGroups updatedWhitelistGroups = whitelistGroupsRepository.save(whitelistGroups);

        Set<String> updatedPlateNumbers = whitelistGroups.getPlateNumbers().stream().collect(Collectors.toSet());
        if (whitelistGroups.getId() == null) {
            for (String updatedPlateNumber : updatedPlateNumbers) {
                Whitelist whitelist = whitelistService.findByPlatenumber(updatedPlateNumber, whitelistGroups.getParkingId());
                if (whitelist != null && whitelistGroups.getForceUpdate()) {
                    whitelistService.deleteById(whitelist.getId());
                }
            }
        } else {
            List<Whitelist> groupWhitelists = whitelistService.listByGroupId(whitelistGroups.getId());
            List<String> groupWhitelistPlateNumbers = new ArrayList<>(groupWhitelists.size());
            for (Whitelist w : groupWhitelists) {
                groupWhitelistPlateNumbers.add(w.getCar().getPlatenumber());
            }

            for (String updatedPlateNumber : updatedPlateNumbers) {
                if (!groupWhitelistPlateNumbers.contains(updatedPlateNumber)) {
                    Whitelist whitelist = whitelistService.findByPlatenumber(updatedPlateNumber, whitelistGroups.getParkingId());
                    if (whitelist != null && whitelistGroups.getForceUpdate()) {
                        whitelistService.deleteById(whitelist.getId());
                    }
                }
            }

            for (Whitelist w : groupWhitelists) {
                if (!updatedPlateNumbers.contains(w.getCar().getPlatenumber())) {
                    whitelistService.deleteById(w.getId());
                }
            }
        }

        for (String updatedPlateNumber : updatedPlateNumbers) {
            whitelistService.saveWhitelistFromGroup(updatedPlateNumber, updatedWhitelistGroups, currentUser, parking);
        }
        return updatedWhitelistGroups;
    }

    @Override
    public void deleteGroup(WhitelistGroups group) {
        whitelistGroupsRepository.delete(group);
    }

    @Override
    public List<WhitelistGroups> listAllWhitelistGroups() throws JsonProcessingException {
        List<WhitelistGroups> whitelistGroupsList = whitelistGroupsRepository.findAll();
        for (WhitelistGroups wg : whitelistGroupsList) {
            wg.setConditionDetail(WhitelistServiceImpl.formConditionDetails(wg, wg.getName()));
        }
        return whitelistGroupsList;
    }

    @SneakyThrows
    @Override
    public Page<WhiteListGroupDto> listByPage(PagingRequest pagingRequest) {
        var list = listAllWhitelistGroups().stream()
                .map(m -> {
                    var whiteLists = whitelistRepository.findByGroupId(m.getId()).stream()
                            .map(w -> Optional.of(w.getCar()).map(c->c.getPlatenumber()).orElse(""))
                            .collect(Collectors.toList());

                    return WhiteListGroupDto.builder()
                            .id(m.getId())
                            .name(m.getName())
                            .parkingName(Optional.of(m.getParking()).map(p -> p.getName()).orElse(""))
                            .size(m.getSize() != null ? m.getSize().toString() : "")
                            .conditionDetail(m.getConditionDetail())
                            .whiteLists(String.join(",",whiteLists))
                            .build();
                })
                .collect(Collectors.toList());
        return getPage(list, pagingRequest);
    }

    @Override
    public void deleteById(Long id) {
        WhitelistGroups whitelistGroups = whitelistGroupsRepository.getWhitelistGroup(id);
        List<Whitelist> groupWhitelists = whitelistService.listByGroupId(whitelistGroups.getId());
        if (groupWhitelists != null) {
            for (Whitelist w : groupWhitelists) {
                whitelistService.deleteById(w.getId());
            }
        }
        whitelistGroupsRepository.deleteById(id);
    }

    @Override
    public WhitelistGroups prepareById(Long id) {
        SimpleDateFormat format = new SimpleDateFormat(dateformat);
        WhitelistGroups whitelistGroups = whitelistGroupsRepository.getWhitelistGroup(id);
        List<String> plateNumbers = new ArrayList<>();
        List<Whitelist> groupWhitelists = whitelistService.listByGroupId(whitelistGroups.getId());
        if (groupWhitelists != null) {
            for (Whitelist w : groupWhitelists) {
                plateNumbers.add(w.getCar().getPlatenumber());
            }
        }
        whitelistGroups.setPlateNumbers(plateNumbers);
        if (Whitelist.Type.PERIOD.equals(whitelistGroups.getType())) {
            if (whitelistGroups.getAccess_start() != null) {
                whitelistGroups.setAccessStartString(format.format(whitelistGroups.getAccess_start()));
            }
            whitelistGroups.setAccessEndString(format.format(whitelistGroups.getAccess_end()));
        }
        if (whitelistGroups.getParking() != null) {
            whitelistGroups.setParkingId(whitelistGroups.getParking().getId());
        }

        return whitelistGroups;
    }

    @Override
    public List<WhiteListGroupDto> listByParkingId(Long parkingId) {
        List<WhitelistGroups> parkingGroups = whitelistGroupsRepository.getWhitelistGroupByParkingId(parkingId);
        return parkingGroups.stream().map(m -> WhiteListGroupDto.builder()
                        .id(m.getId())
                        .name(m.getName())
                        .build())
                .collect(Collectors.toList());
    }

    private Page<WhiteListGroupDto> getPage(List<WhiteListGroupDto> list, PagingRequest pagingRequest) {
        var filtered = list.stream()
                .sorted(sort(pagingRequest))
                .filter(filter(pagingRequest))
                .skip(pagingRequest.getStart())
                .limit(pagingRequest.getLength())
                .collect(Collectors.toList());
        var count = list.stream()
                .filter(filter(pagingRequest))
                .count();

        var page = new Page<>(filtered);
        page.setRecordsFiltered((int) count);
        page.setRecordsTotal((int) count);
        page.setDraw(pagingRequest.getDraw());
        return page;
    }

    private Predicate<WhiteListGroupDto> filter(PagingRequest pagingRequest) {
        if (pagingRequest.getSearch() == null || StringUtils.isNullOrEmpty(pagingRequest.getSearch()
                .getValue())) {
            return model -> true;
        }

        String value = pagingRequest.getSearch().getValue();

        return model -> (model.getName() != null && model.getName().toLowerCase().contains(value.toLowerCase())
                || (model.getParkingName() != null && model.getParkingName().toLowerCase().contains(value.toLowerCase()))
                || (model.getWhiteLists() != null && model.getWhiteLists().toLowerCase().contains(value.toLowerCase())));
    }

    private Comparator<WhiteListGroupDto> sort(PagingRequest pagingRequest) {
        if (pagingRequest.getOrder() == null) {
            return EMPTY_COMPARATOR;
        }

        try {
            Order order = pagingRequest.getOrder().get(0);

            int columnIndex = order.getColumn();
            Column column = pagingRequest.getColumns().get(columnIndex);

            Comparator<WhiteListGroupDto> comparator = WhiteListGroupComparators.getComparator(column.getData(), order.getDir());
            return Objects.requireNonNullElse(comparator, EMPTY_COMPARATOR);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return EMPTY_COMPARATOR;
    }
}

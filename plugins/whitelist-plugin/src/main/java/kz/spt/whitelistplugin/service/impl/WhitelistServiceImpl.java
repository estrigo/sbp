package kz.spt.whitelistplugin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.lib.model.Cars;
import kz.spt.lib.utils.StaticValues;
import kz.spt.whitelistplugin.model.WhitelistGroups;
import kz.spt.lib.service.CarStateService;
import kz.spt.whitelistplugin.repository.WhitelistGroupsRepository;
import kz.spt.whitelistplugin.service.RootServicesGetterService;
import kz.spt.lib.service.ParkingService;
import kz.spt.whitelistplugin.model.Whitelist;
import kz.spt.whitelistplugin.repository.WhitelistRepository;
import kz.spt.whitelistplugin.service.WhitelistService;
import org.pf4j.util.StringUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
public class WhitelistServiceImpl implements WhitelistService {

    private final String dateformat = "yyyy-MM-dd'T'HH:mm";
    private CarStateService carStateService;
    private ParkingService parkingService;
    private WhitelistRepository whitelistRepository;
    private WhitelistGroupsRepository whitelistGroupsRepository;
    private RootServicesGetterService rootServicesGetterService;

    public WhitelistServiceImpl(WhitelistRepository whitelistRepository, WhitelistGroupsRepository whitelistGroupsRepository,
                                RootServicesGetterService rootServicesGetterService) {
        this.whitelistRepository = whitelistRepository;
        this.whitelistGroupsRepository = whitelistGroupsRepository;
        this.rootServicesGetterService = rootServicesGetterService;
    }

    @Override
    public void saveWhitelist(Whitelist whitelist, UserDetails currentUser) throws Exception {
        whitelist.setPlatenumber(whitelist.getPlatenumber().toUpperCase());

        if (whitelist.getGroupId() != null) {
            WhitelistGroups group = whitelistGroupsRepository.getOne(whitelist.getGroupId());
            whitelist.setGroup(group);
        } else if (whitelist.getGroup() != null) {
            whitelist.setGroup(null);
        }

        Cars car = rootServicesGetterService.getCarsService().createCar(whitelist.getPlatenumber());
        whitelist.setCar(car);

        SimpleDateFormat format = new SimpleDateFormat(dateformat);
        if (Whitelist.Type.PERIOD.equals(whitelist.getType())) {
            if (StringUtils.isNotNullOrEmpty(whitelist.getAccessStartString())) {
                whitelist.setAccess_start(format.parse(whitelist.getAccessStartString()));
            }
            if (StringUtils.isNotNullOrEmpty(whitelist.getAccessEndString())) {
                whitelist.setAccess_end(format.parse(whitelist.getAccessEndString()));
            }
        } else {
            whitelist.setAccess_start(null);
            whitelist.setAccess_end(null);
        }
        if (whitelist.getId() != null) {
            whitelist.setUpdatedUser(currentUser.getUsername());
        } else {
            whitelist.setCreatedUser(currentUser.getUsername());
        }
        whitelistRepository.save(whitelist);
    }

    @Override
    public Iterable<Whitelist> listAllWhitelist() {
        return whitelistRepository.findAll();
    }

    @Override
    public List<Whitelist> listByGroupId(Long groupId) {
        return whitelistRepository.findByGroupId(groupId);
    }

    @Override
    public ArrayNode hasAccess(String platenumber, Date date) {

        ObjectMapper objectMapper = new ObjectMapper();
        ArrayNode arrayNode = objectMapper.createArrayNode();

        Cars car = rootServicesGetterService.getCarsService().findByPlatenumber(platenumber);
        if (car != null) {
            List<Whitelist> whitelists = whitelistRepository.findValidWhiteListByCar(car, date);
            List<Whitelist> groupWhitelists = whitelistRepository.findValidWhiteListGroupByCar(car, date);
            SimpleDateFormat format =  new SimpleDateFormat("dd.MM.yyyy hh:mm:ss");

            for(Whitelist whitelist : whitelists){
                ObjectNode objectNode = objectMapper.createObjectNode();
                objectNode.put("id", whitelist.getId());
                objectNode.put("plateNumber", whitelist.getCar().getPlatenumber());
                objectNode.put("type", whitelist.getType().toString());
                if(Whitelist.Type.PERIOD.equals(whitelist.getType())){
                    if(whitelist.getAccess_start() != null){
                        objectNode.put("accessStart", format.format(whitelist.getAccess_start()));
                    }
                    if(whitelist.getAccess_end() != null){
                        objectNode.put("accessEnd", format.format(whitelist.getAccess_end()));
                    }
                }
                arrayNode.add(objectNode);
            }
            for(Whitelist whitelist : groupWhitelists){
                ObjectNode objectNode = objectMapper.createObjectNode();
                objectNode.put("id", whitelist.getGroup().getId());
                objectNode.put("plateNumber", whitelist.getCar().getPlatenumber());
                objectNode.put("groupId", whitelist.getGroup().getId());
                objectNode.put("groupName", whitelist.getGroup().getName());
                objectNode.put("type", whitelist.getGroup().getType().toString());
                if(Whitelist.Type.PERIOD.equals(whitelist.getGroup().getType())){
                    if(whitelist.getGroup().getAccess_start() != null){
                        objectNode.put("accessStart", format.format(whitelist.getGroup().getAccess_start()));
                    }
                    if(whitelist.getGroup().getAccess_end() != null){
                        objectNode.put("accessEnd", format.format(whitelist.getGroup().getAccess_end()));
                    }
                }
                arrayNode.add(objectNode);
            }
        }

        return arrayNode;
    }

    @Override
    public Whitelist prepareById(Long id) {
        SimpleDateFormat format = new SimpleDateFormat(dateformat);
        Whitelist whitelist = whitelistRepository.getWithCarAndGroup(id);
        whitelist.setPlatenumber(whitelist.getCar().getPlatenumber());
        if (whitelist.getGroup() != null) {
            whitelist.setGroupId(whitelist.getGroup().getId());
        }
        if (Whitelist.Type.PERIOD.equals(whitelist.getType())) {
            if (whitelist.getAccess_start() != null) {
                whitelist.setAccessStartString(format.format(whitelist.getAccess_start()));
            }
            whitelist.setAccessEndString(format.format(whitelist.getAccess_end()));
        }
        return whitelist;
    }

    @Override
    public void deleteById(Long id) {
        whitelistRepository.deleteById(id);
    }

    @Override
    public void saveWhitelistFromGroup(String plateNumber, WhitelistGroups group, String currentUser) {
        Cars car = rootServicesGetterService.getCarsService().createCar(plateNumber);

        Whitelist whitelist = whitelistRepository.findWhiteListByCar(car);

        if (whitelist == null) {
            whitelist = new Whitelist();
            whitelist.setGroup(group);
            whitelist.setUpdatedUser(currentUser);
            whitelist.setCar(car);
        } else {

            if(!group.getId().equals(whitelist.getGroupId())) {
                // alert
                whitelist.setGroup(group);
            }
        }
        whitelistRepository.save(whitelist);
    }
}

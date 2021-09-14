package kz.spt.whitelistplugin.service.impl;


import kz.spt.lib.model.Cars;
import kz.spt.whitelistplugin.model.WhitelistGroups;
import kz.spt.whitelistplugin.repository.WhitelistGroupsRepository;
import kz.spt.whitelistplugin.service.RootServicesGetterService;
import kz.spt.whitelistplugin.service.WhitelistGroupsService;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class WhitelistGroupsServiceImpl implements WhitelistGroupsService {

    private WhitelistGroupsRepository whitelistGroupsRepository;
    private RootServicesGetterService rootServicesGetterService;

    public WhitelistGroupsServiceImpl(WhitelistGroupsRepository whitelistGroupsRepository, RootServicesGetterService  rootServicesGetterService) {
        this.whitelistGroupsRepository = whitelistGroupsRepository;
        this.rootServicesGetterService = rootServicesGetterService;
    }

    @Override
    public WhitelistGroups findById(Long id) {
        return whitelistGroupsRepository.getOne(id);
    }

    @Override
    public WhitelistGroups getWithCars(Long id) {
        return whitelistGroupsRepository.getWhitelistGroupsWithCars(id);
    }

    @Override
    public WhitelistGroups createGroup(String name, String[] carList, String username) {

        WhitelistGroups groups = new WhitelistGroups();
        groups.setName(name);

        Set<String> plateNumbers = Arrays.stream(carList).collect(Collectors.toSet());

        Set<Cars> cars = new HashSet<>();
        for(String plateNumber: plateNumbers){
            Cars car = rootServicesGetterService.getCarsService().createCar(plateNumber);
            cars.add(car);
        }
        groups.setCars(cars);
        groups.setUpdatedUser(username);
        whitelistGroupsRepository.save(groups);

        return groups;
    }

    @Override
    public void updateGroup(Long groupId, String name, String[] carList, String username) {
        WhitelistGroups groups = findById(groupId);
        groups.setName(name);

        Set<String> plateNumbers = Arrays.stream(carList).collect(Collectors.toSet());

        Set<Cars> cars = new HashSet<>();
        for(String plateNumber: plateNumbers){
            Cars car = rootServicesGetterService.getCarsService().createCar(plateNumber);
            cars.add(car);
        }
        groups.setCars(cars);
        groups.setUpdatedUser(username);
        whitelistGroupsRepository.save(groups);
    }

    @Override
    public void deleteGroup(WhitelistGroups group) {
        whitelistGroupsRepository.delete(group);
    }
}

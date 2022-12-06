package kz.spt.carmodelplugin.service.impl;

import kz.spt.carmodelplugin.repository.CarDimensionsRepository;
import kz.spt.carmodelplugin.service.CarDimensionsService;
import kz.spt.lib.model.Dimensions;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Log
@Service
@Transactional(noRollbackFor = Exception.class)
@RequiredArgsConstructor
public class CarDimensionsServiceImpl implements CarDimensionsService {
    private final CarDimensionsRepository carDimensionsRepository;


    @Override
    public void saveCarDimensions (Dimensions dimensions, UserDetails currentUser) {
        LocalDateTime localDateTime = LocalDateTime.now();
        dimensions.setUpdatedTime(localDateTime);
        dimensions.setUpdatedBy(currentUser.getUsername());
        carDimensionsRepository.save(dimensions);
    }

    @Override
    public List<Dimensions> listDimensions() {
        return carDimensionsRepository.findAll();
    }

    @Override
    public Dimensions getById(Long id) {
        Optional<Dimensions> byId = carDimensionsRepository.findById(id);
        Dimensions dimensions = byId.get();
        return dimensions;
    }

    @Override
    public void deleteDimension (Long id) {
        Dimensions dimensions = carDimensionsRepository.getById(id);
        carDimensionsRepository.delete(dimensions);
    }

    @Override
    public void updateCarDimension(long id, Dimensions dimensions, UserDetails currentUser) {
        Dimensions dimension = getById(id);
        dimension.setCarClassification(dimensions.getCarClassification());
        dimension.setUpdatedBy(currentUser.getUsername());
        LocalDateTime localDateTime = LocalDateTime.now();
        dimension.setUpdatedTime(localDateTime);

        try {
            carDimensionsRepository.save(dimension);
        } catch (Exception e) {
        }
    }
}

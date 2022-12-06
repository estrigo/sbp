package kz.spt.carmodelplugin.service;

import kz.spt.carmodelplugin.utils.ExcelUtilsCarModel;
import kz.spt.lib.model.CarModel;
import kz.spt.lib.model.Dimensions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.util.Pair;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@Transactional(noRollbackFor = Exception.class)
@RequiredArgsConstructor
public class CarModelFileServices {
    private final CarDimensionsService carDimensionsService;
    private final CarModelServicePl carModelServicePl;
    public void store(MultipartFile file, Dimensions dimensions, UserDetails currentUser){
        try {
            Dimensions dimensionById = carDimensionsService.getById(dimensions.getId());
            List<Pair<CarModel, String>> carModellists = ExcelUtilsCarModel.parseExcelFileWhiteList(file.getInputStream(), dimensionById);

            for (int i = 0; i < carModellists.size(); i++) {
                CarModel carModel = carModellists.get(i).getFirst();
                LocalDateTime localDateTime = LocalDateTime.now();
                carModel.setUpdatedTime(localDateTime);
                carModel.setUpdatedBy(currentUser.getUsername());
                if (carModel.getDimensions().getId() != null) {
                    carModel.setType(Math.toIntExact(carModel.getDimensions().getId()));
                }
                try {
                    carModelServicePl.saveCarModel(carModel, currentUser);
                } catch (Exception e) {
                    log.info("the next car model was not saved: " + carModel.getDimensions());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("FAIL! -> message = " + e.getMessage());
        }
    }
}

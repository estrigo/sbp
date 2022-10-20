package kz.spt.carmodelplugin.service;

import kz.spt.lib.model.Dimensions;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

public interface CarDimensionsService {

    void saveCarDimensions (Dimensions dimensions, UserDetails currentUser);
    List<Dimensions> listDimensions();

    Dimensions getById(Long id);
    void deleteDimension (Long id);

    void updateCarDimension(long id, Dimensions dimensions, UserDetails currentUser);
}

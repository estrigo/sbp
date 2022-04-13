package kz.spt.lib.service;

import kz.spt.lib.model.CarModel;
import org.springframework.stereotype.Repository;

@Repository
public interface CarModelService {

     CarModel getByModel(String model);
}

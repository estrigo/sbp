package kz.spt.rateplugin.service;

import kz.spt.lib.model.Dimensions;
import java.util.List;

public interface DimensionsService {
    List<Dimensions> findAll();

    Dimensions findById(String id);
}

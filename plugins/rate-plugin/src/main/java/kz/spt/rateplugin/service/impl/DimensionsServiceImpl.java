
package kz.spt.rateplugin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.spt.lib.model.Dimensions;
import kz.spt.rateplugin.repository.DimensionsRepository;
import kz.spt.rateplugin.service.DimensionsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Log
@Service
@Transactional(noRollbackFor = Exception.class)
@RequiredArgsConstructor
public class DimensionsServiceImpl implements DimensionsService {
    private final DimensionsRepository dimensionsRepository;
    private ObjectMapper objectMapper = new ObjectMapper();
    @Override
    public List<Dimensions> findAll() {
        return dimensionsRepository.findAll();
    }


    @Override
    public Dimensions findById(String id) {
        Optional<Dimensions> byId = dimensionsRepository.findById(Long.valueOf(id));
        Dimensions dimensions = byId.get();
        return dimensions;
    }

    @Override
    public void deleteDimensionsById(Long id) {
        dimensionsRepository.deleteById(id);
    }

}
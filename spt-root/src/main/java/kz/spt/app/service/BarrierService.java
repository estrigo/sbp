package kz.spt.app.service;


import kz.spt.api.model.Barrier;

import java.io.IOException;
import java.text.ParseException;
import java.util.Map;

public interface BarrierService {

    Barrier getBarrierById(Long id);

    void saveBarrier(Barrier barrier);

    Boolean openBarrier(Barrier barrier, Map<String, Object> properties) throws IOException, ParseException, InterruptedException;
}

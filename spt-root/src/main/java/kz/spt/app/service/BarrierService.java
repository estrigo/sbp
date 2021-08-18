package kz.spt.app.service;


import kz.spt.api.model.Barrier;

import java.io.IOException;
import java.text.ParseException;

public interface BarrierService {

    Barrier getBarrierById(Long id);

    void saveBarrier(Barrier barrier);

    void openBarrier(Barrier barrier) throws IOException, ParseException;
}

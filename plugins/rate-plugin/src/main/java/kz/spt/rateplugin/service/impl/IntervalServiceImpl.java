
package kz.spt.rateplugin.service.impl;

import kz.spt.lib.model.Parking;
import kz.spt.rateplugin.model.IntervalRate;
import kz.spt.rateplugin.repository.IntervalRateRepository;
import kz.spt.rateplugin.service.IntervalRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;


@Log
@Service
@Transactional(noRollbackFor = Exception.class)
@RequiredArgsConstructor
public class IntervalServiceImpl implements IntervalRateService {

    private final IntervalRateRepository intervalRateRepository;

    @Override
    public void saveIntervalRate(IntervalRate intervalRate) {
        intervalRateRepository.save(intervalRate);
    }

    @Override
    public IntervalRate findById(Long id) {
        Optional<IntervalRate> optionalIntervalRate = intervalRateRepository.findById(id);
        return optionalIntervalRate.isPresent() ? optionalIntervalRate.get() : null;
    }
}

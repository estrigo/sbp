
package kz.spt.rateplugin.service;

import kz.spt.rateplugin.model.IntervalRate;

public interface IntervalRateService {

    void saveIntervalRate (IntervalRate intervalRate);
    IntervalRate findById(Long id);


}
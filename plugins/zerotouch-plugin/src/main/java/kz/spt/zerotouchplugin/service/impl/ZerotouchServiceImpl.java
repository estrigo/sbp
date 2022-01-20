package kz.spt.zerotouchplugin.service.impl;

import kz.spt.zerotouchplugin.service.ZerotouchService;
import org.springframework.stereotype.Service;

@Service
public class ZerotouchServiceImpl implements ZerotouchService {

    @Override
    public Boolean checkZeroTouchValid(String plateNumber, Long carStateId) {

        /*
        1. Token check
        2. Call method if result true return true
        3. save request and respnse data and carStateId
        */

        return false;
    }
}

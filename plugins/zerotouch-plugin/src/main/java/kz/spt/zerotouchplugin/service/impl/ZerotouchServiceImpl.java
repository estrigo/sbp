package kz.spt.zerotouchplugin.service.impl;

import kz.spt.zerotouchplugin.service.ZerotouchService;
import org.springframework.stereotype.Service;

@Service
public class ZerotouchServiceImpl implements ZerotouchService {

    @Override
    public Boolean checkZeroTouchValid(String plateNumber) {
        return false;
    }
}

package kz.spt.zerotouchplugin.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;

public interface ZerotouchService {

    Boolean checkZeroTouchValid(String plateNumber, BigDecimal rate, Long carStateId) throws IOException, URISyntaxException;
}

package kz.spt.app.service.impl;

import kz.spt.lib.model.Camera;
import kz.spt.lib.model.CarState;
import kz.spt.lib.service.CarStateService;
import kz.spt.app.repository.CarStateRepository;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class CarStateServiceImpl implements CarStateService {

    private CarStateRepository carStateRepository;


    public CarStateServiceImpl(CarStateRepository carStateRepository){
        this.carStateRepository = carStateRepository;
    }

    @Override
    public void createINState(String carNumber, Date inTimestamp, Camera camera) {
        CarState carState = new CarState();
        carState.setCarNumber(carNumber);
        carState.setInTimestamp(inTimestamp);
        carState.setType(camera.getGate().getParking().getParkingType());
        carState.setInChannelIp(camera.getIp());
        carState.setParking(camera.getGate().getParking());
        carState.setInGate(camera.getGate());
        carState.setInBarrier(camera.getGate().getBarrier());
        carStateRepository.save(carState);
    }

    @Override
    public void createOUTState(String carNumber, Date outTimestamp, Camera camera, Long paymentId, Long amount, Boolean paid) {
        CarState carState = getLastNotLeft(carNumber);
        if(carState == null){
            carState = new CarState();
            carState.setCarNumber(carNumber);
        }
        carState.setOutTimestamp(outTimestamp);
        carState.setOutChannelIp(camera.getIp());
        carState.setOutGate(camera.getGate());
        carState.setOutBarrier(camera.getGate().getBarrier());
        carState.setPayment(paymentId);
        carState.setAmount(amount);
        carState.setPayment(paymentId);
        carState.setPaid(paid);
        carStateRepository.save(carState);
    }

    @Override
    public Boolean checkIsLastEnteredNotLeft(String carNumber) {
        return getLastNotLeft(carNumber) != null;
    }

    @Override
    public CarState getLastNotLeft(String carNumber) {
        return carStateRepository.getCarStateNotLeft(carNumber);
    }

    @Override
    public Iterable<CarState> getAllNotLeft() {
        return carStateRepository.getAllCarStateNotLeft();
    }


}

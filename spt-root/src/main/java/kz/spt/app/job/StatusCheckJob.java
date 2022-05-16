package kz.spt.app.job;

import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException;
import kz.spt.app.model.dto.BarrierStatusDto;
import kz.spt.app.model.dto.CameraStatusDto;
import kz.spt.app.model.dto.GateStatusDto;
import kz.spt.app.service.BarrierService;
import kz.spt.app.service.GateService;
import kz.spt.app.thread.GateStatusCheckThread;
import kz.spt.lib.model.Barrier;
import kz.spt.lib.model.Gate;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Log
@Component
public class StatusCheckJob {

    @Autowired
    private GateService gateService;

    @Autowired
    private BarrierService barrierService;

    public static Boolean emergencyModeOn = false;

    public static Map<Long, Boolean> isGatesProcessing = new ConcurrentHashMap<>();
    public static Queue<GateStatusDto> globalGateDtos = new ConcurrentLinkedQueue<>();

    @Scheduled(fixedDelayString = "${status.check.fixedDelay}", initialDelay = 5000)
    public void scheduleFixedDelayTask() throws UnknownHostException, ModbusIOException {
        if(globalGateDtos.isEmpty()){
            refreshGlobalGateIds();
        }
        for (GateStatusDto gateStatusDto : globalGateDtos) {
            if(!isGatesProcessing.containsKey(gateStatusDto.gateId) || !isGatesProcessing.get(gateStatusDto.gateId)){
                BarrierStatusDto barrier = gateStatusDto.barrier;
                CameraStatusDto cameraStatusDto = gateStatusDto.frontCamera;

                if(barrier != null && cameraStatusDto != null && Barrier.SensorsType.MANUAL.equals(barrier.sensorsType)) { // Данные шлагбаума и камеры заполнены
                    isGatesProcessing.put(gateStatusDto.gateId, true);
                    new GateStatusCheckThread(gateStatusDto, barrierService).start();
                }
            }
        }
    }

    private void refreshGlobalGateIds() throws UnknownHostException, ModbusIOException {
        globalGateDtos = new ConcurrentLinkedQueue<>();
        List<Gate> allGates = (List<Gate>) gateService.listAllGatesWithDependents();
        for (Gate gate : allGates){
            if(!isGatesProcessing.containsKey(gate.getId())){
                globalGateDtos.add(GateStatusDto.fromGate(gate, allGates, barrierService));
                isGatesProcessing.put(gate.getId(), false);
            }
        }
    }

    public static void emptyGlobalGateDtos(){
        isGatesProcessing = new ConcurrentHashMap<>();
        globalGateDtos = new ConcurrentLinkedQueue<>();
    }

    public static GateStatusDto findGateStatusDtoById(Long gateId){
        for(GateStatusDto gate: StatusCheckJob.globalGateDtos){
            if(gate.gateId == gateId){
                return gate;
            }
        }
        return null;
    }

    public static CameraStatusDto findCameraStatusDtoByIp(String cameraIp){
        for(GateStatusDto gate: StatusCheckJob.globalGateDtos){
            if(gate.frontCamera != null && cameraIp.equals(gate.frontCamera.ip)){
                return gate.frontCamera;
            } else if(gate.frontCamera2 != null && cameraIp.equals(gate.frontCamera2.ip)){
                return gate.frontCamera2;
            } else if(gate.backCamera != null && cameraIp.equals(gate.backCamera.ip)){
                return gate.backCamera;
            }
        }
        return null;
    }
}

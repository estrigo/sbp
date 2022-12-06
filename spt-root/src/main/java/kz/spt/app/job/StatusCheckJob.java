package kz.spt.app.job;

import com.google.gson.JsonArray;
import kz.spt.app.model.dto.BarrierStatusDto;
import kz.spt.app.model.dto.CameraStatusDto;
import kz.spt.app.model.dto.GateStatusDto;
import kz.spt.app.model.dto.SensorStatusDto;
import kz.spt.app.service.BarrierService;
import kz.spt.app.service.GateService;
import kz.spt.app.thread.GateStatusCheckThread;
import kz.spt.lib.model.Barrier;
import kz.spt.lib.model.Gate;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Log
@Component
public class StatusCheckJob {

    private GateService gateService;

    private BarrierService barrierService;

    @Autowired
    public StatusCheckJob(GateService gateService, BarrierService barrierService){
        this.gateService = gateService;
        this.barrierService = barrierService;
    }

    public static Boolean emergencyModeOn = false;

    public static Map<Long, Boolean> isGatesProcessing = new ConcurrentHashMap<>();
    public static Queue<GateStatusDto> globalGateDtos = new ConcurrentLinkedQueue<>();

    public static Map<String, BarrierStatusDto> barrierStatusDtoMap = new ConcurrentHashMap<>();
    public static Map<String, SensorStatusDto> loopStatusDtoMap = new ConcurrentHashMap<>();
    public static Map<String, SensorStatusDto> pheStatusDtoMap = new ConcurrentHashMap<>();

    @Scheduled(fixedDelayString = "${status.check.fixedDelay}", initialDelay = 5000)
    public void scheduleFixedDelayTask() {
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

    private void refreshGlobalGateIds() {
        globalGateDtos = new ConcurrentLinkedQueue<>();
        List<Gate> allGates = (List<Gate>) gateService.listAllGatesWithDependents();
        for (Gate gate : allGates){
            if(!isGatesProcessing.containsKey(gate.getId())){
                globalGateDtos.add(GateStatusDto.fromGate(gate, allGates));
                isGatesProcessing.put(gate.getId(), false);
            }
        }
    }

    public static void emptyGlobalGateDtos() {
        isGatesProcessing = new ConcurrentHashMap<>();
        globalGateDtos = new ConcurrentLinkedQueue<>();
        barrierStatusDtoMap = new ConcurrentHashMap<>();
        loopStatusDtoMap = new ConcurrentHashMap<>();
        pheStatusDtoMap = new ConcurrentHashMap<>();
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

    public static CameraStatusDto findCameraStatusDtoById(Long cameraId){
        for(GateStatusDto gate: StatusCheckJob.globalGateDtos){
            if(gate.frontCamera != null && cameraId.equals(gate.frontCamera.id)){
                return gate.frontCamera;
            } else if(gate.frontCamera2 != null && cameraId.equals(gate.frontCamera2.id)){
                return gate.frontCamera2;
            } else if(gate.backCamera != null && cameraId.equals(gate.backCamera.id)){
                return gate.backCamera;
            }
        }
        return null;
    }

    public static boolean checkIpExist(String barrierIp){
        for(GateStatusDto gate: StatusCheckJob.globalGateDtos){
            if(gate.barrier != null && gate.barrier.ip != null && gate.barrier.ip.equals(barrierIp)){
                return true;
            }
        }
        return false;
    }

    public static List<Long> getBarrierOpenCameraIds(String barrierIp, Integer pin, String oid){
        List<Long> cameraIds = new ArrayList<>();
        for (GateStatusDto gateStatusDto : globalGateDtos) {
            if(gateStatusDto.barrier != null){
                Boolean satisfy =
                        (barrierIp.equals(gateStatusDto.barrier.ip) && pin != null && Barrier.BarrierType.MODBUS.equals(gateStatusDto.barrier.type) && pin.equals(gateStatusDto.barrier.modbusOpenRegister))
                        || (barrierIp.equals(gateStatusDto.barrier.ip) && oid != null && Barrier.BarrierType.SNMP.equals(gateStatusDto.barrier.type) && oid.equals(gateStatusDto.barrier.openOid));

                if(satisfy){
                    if(gateStatusDto.frontCamera != null && gateStatusDto.frontCamera.ip != null){
                        cameraIds.add(gateStatusDto.frontCamera.id);
                    }
                    if(gateStatusDto.backCamera != null && gateStatusDto.backCamera.ip != null){
                        cameraIds.add(gateStatusDto.backCamera.id);
                    }
                    if(gateStatusDto.frontCamera2 != null && gateStatusDto.frontCamera2.ip != null){
                        cameraIds.add(gateStatusDto.frontCamera2.id);
                    }
                }
            }
        }
        return cameraIds;
    }
}

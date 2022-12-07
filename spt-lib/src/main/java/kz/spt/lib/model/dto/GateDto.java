package kz.spt.lib.model.dto;

import kz.spt.lib.model.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GateDto {
    private Long id;
    private String name;
    private String description;
    private Boolean notControlBarrier = false;

    @Enumerated(EnumType.STRING)
    private Gate.GateType gateType;

    private Long parkingId;
    private List<Long> cameraIdList;
    private Long barrierId;
    private Long controllerId;
    @CreationTimestamp
    private Date created;
    @UpdateTimestamp
    private Date updated;
    private String qrPanelIp;
    private String tabloIp;
    private Integer parkingSpaceNumber;


    public static GateDto fromGate(Gate gate){
        GateDto dto = new GateDto();
        dto.cameraIdList = new ArrayList<>();

        dto.id = gate.getId();
        dto.name = gate.getName();
        dto.description = gate.getDescription();
        dto.notControlBarrier = gate.getNotControlBarrier();
        dto.gateType = gate.getGateType();

        if(gate.getParking()!=null)
        dto.parkingId = gate.getParking().getId();

        if (!gate.getCameraList().isEmpty())
            for (Camera camera: gate.getCameraList())
                if (camera != null)
                    dto.cameraIdList.add(camera.getId());

        if(gate.getBarrier()!=null)
        dto.barrierId = gate.getBarrier().getId();

        if(gate.getController()!=null)
        dto.controllerId = gate.getController().getId();

        dto.created = gate.getCreated();
        dto.updated = gate.getUpdated();
        dto.qrPanelIp = gate.getQrPanelIp();
        dto.tabloIp = gate.getTabloIp();
        dto.parkingSpaceNumber = gate.getParkingSpaceNumber();
        return dto;
    }

    public static List<GateDto> fromGates(List<Gate> gates){
        List<GateDto> gateDtos = new ArrayList<>(gates.size());
        for(Gate gate : gates){
            gateDtos.add(fromGate(gate));
        }
        return gateDtos;
    }
}

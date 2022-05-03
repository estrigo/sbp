package kz.spt.lib.service;

import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusNumberException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusProtocolException;
import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.lib.model.EventLog;
import kz.spt.lib.model.dto.EventFilterDto;
import kz.spt.lib.model.dto.EventsDto;

import java.text.ParseException;
import java.util.Map;

public interface EventLogService {

    enum ArmEventType {
        Photo,
        CarEvent,
        Lp
    }

    void createEventLog(String objectClass, Long objectId, Map<String, Object> properties, String description, String descriptionEn);

    void sendSocketMessage(ArmEventType eventType, EventLog.StatusType eventStatus, Long gateId, String plateNumber, String message, String messageEng);

    Iterable<EventLog> listByType(EventLog.EventType type);

    EventLog getById(Long id);

    Page<EventsDto> getEventLogs(PagingRequest pagingRequest, EventFilterDto eventFilterDto) throws ParseException;

    String getEventExcel(EventFilterDto eventFilterDto) throws Exception;

    void save(EventLog eventLog);

    String getApplicationPropertyValue(String prortyName) throws ModbusIOException, ModbusProtocolException, ModbusNumberException, InterruptedException;

    String findLastNotEnoughFunds(Long gateId);

    String findLastWithDebts(Long gateId);
}

package kz.spt.lib.service;

import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusNumberException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusProtocolException;
import jdk.jfr.Event;
import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.lib.model.EventLog;
import kz.spt.lib.model.dto.EventFilterDto;
import kz.spt.lib.model.dto.EventLogExcelDto;
import kz.spt.lib.model.dto.EventsDto;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

public interface EventLogService {

    enum ArmEventType {
        Photo,
        CarEvent,
        Lp
    }

    void createEventLog(String objectClass, Long objectId, Map<String, Object> properties, String description, String descriptionEn);

    void createEventLog(String objectClass, Long objectId, Map<String, Object> properties, String description, String descriptionEn, EventLog.EventType eventType);

    void sendSocketMessage(ArmEventType eventType, EventLog.StatusType eventStatus, Long gateId, String plateNumber, String message, String messageEng);

    Iterable<EventLog> listByType(EventLog.EventType type);

    EventLog getById(Long id);

    Page<EventsDto> getEventLogs(PagingRequest pagingRequest, EventFilterDto eventFilterDto) throws ParseException;

    List<EventLogExcelDto> getEventExcel(EventFilterDto eventFilterDto) throws Exception;

    void save(EventLog eventLog);

    String getApplicationPropertyValue(String prortyName) throws ModbusIOException, ModbusProtocolException, ModbusNumberException, InterruptedException;

    String findLastNotEnoughFunds(Long gateId);

    String findLastWithDebts(Long gateId);
}

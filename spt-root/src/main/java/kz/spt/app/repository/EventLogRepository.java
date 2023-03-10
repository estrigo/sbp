package kz.spt.app.repository;

import kz.spt.lib.model.EventLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface EventLogRepository extends JpaRepository<EventLog, Long>, JpaSpecificationExecutor<EventLog> {

    @Query("from EventLog el order by el.created desc")
    Iterable<EventLog> listAllEvents();

    @Query("from EventLog el where el.eventType = :type order by el.id desc")
    Iterable<EventLog> listByType(@Param("type") EventLog.EventType type);

    List<EventLog> findByEventTypeIn(List<EventLog.EventType> types);

    List<EventLog> findByEventTypeIn(List<EventLog.EventType> types, Pageable pageable);

    List<EventLog> findAllByCreatedBetweenAndEventTypeInOrderByIdDesc(Date dateFrom, Date dateTo,
                                                                List<EventLog.EventType> types, Pageable pageable);

    Long countByCreatedBetweenAndEventTypeIn(Date dateFrom, Date dateTo, List<EventLog.EventType> types);

    Long countByEventTypeIn(List<EventLog.EventType> types);


    @Query("from EventLog el where el.created >= :fromDate and el.objectClass = :className and el.objectId = :gateId order by el.id desc")
    List<EventLog> getEventsFromDate(@Param("fromDate") Date fromDate, @Param("className") String className, @Param("gateId") Long gateId);
}

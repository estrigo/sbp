package kz.spt.app.repository;

import kz.spt.api.model.EventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface EventLogRepository extends JpaRepository<EventLog, Long> {

    @Query("from EventLog el order by el.created desc")
    Iterable<EventLog> listAllEvents();
}

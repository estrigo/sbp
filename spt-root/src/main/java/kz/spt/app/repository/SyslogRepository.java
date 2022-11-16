package kz.spt.app.repository;

import kz.spt.lib.model.Syslog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SyslogRepository extends JpaRepository<Syslog, Long> {

}

package kz.spt.zerotouchplugin.repository;

import kz.spt.zerotouchplugin.model.ZeroTouchLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ZeroTouchLogRepository extends JpaRepository<ZeroTouchLog, Long> {
    ZeroTouchLog findFirstByCarStateIdOrderByIdDesc(long carStateId);
}

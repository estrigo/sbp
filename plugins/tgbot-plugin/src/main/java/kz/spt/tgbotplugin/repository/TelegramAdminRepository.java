package kz.spt.tgbotplugin.repository;

import kz.spt.lib.model.telegram.TelegramAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TelegramAdminRepository extends JpaRepository<TelegramAdmin, Long> {
    TelegramAdmin getTelegramAdminByPhoneNumber(String phoneNumber);
}

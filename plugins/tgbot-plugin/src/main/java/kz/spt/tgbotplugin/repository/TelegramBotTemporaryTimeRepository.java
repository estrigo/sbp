package kz.spt.tgbotplugin.repository;

import kz.spt.lib.model.telegram.TelegramBotTemporaryTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TelegramBotTemporaryTimeRepository extends JpaRepository<TelegramBotTemporaryTime, Long> {
    TelegramBotTemporaryTime getTelegramBotTemporaryTimeById(Long id);
}
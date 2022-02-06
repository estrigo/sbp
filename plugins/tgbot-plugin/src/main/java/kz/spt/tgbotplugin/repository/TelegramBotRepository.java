package kz.spt.tgbotplugin.repository;

import kz.spt.lib.model.telegram.TelegramBot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TelegramBotRepository extends JpaRepository<TelegramBot, Integer> {
    TelegramBot getTelegramBotByUsername(String username);
    TelegramBot getTelegramBotById(int id);
}
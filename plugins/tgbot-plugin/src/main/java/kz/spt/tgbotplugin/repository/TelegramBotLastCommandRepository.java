package kz.spt.tgbotplugin.repository;

import kz.spt.lib.model.telegram.TelegramLastCommand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TelegramBotLastCommandRepository extends JpaRepository<TelegramLastCommand, Long> {
    List<TelegramLastCommand> getTelegramLastCommandByChatId(String chatId);
}

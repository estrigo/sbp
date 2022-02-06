package kz.spt.tgbotplugin.repository;

import kz.spt.lib.model.telegram.TelegramBotChatId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TelegramBotChatIdRepository extends JpaRepository<TelegramBotChatId, Long> {
    TelegramBotChatId getTelegramBotChatIdByChatId(String chatId);
    TelegramBotChatId getTelegramBotChatIdById(Long id);
}

package kz.spt.tgbotplugin.component;

import org.springframework.stereotype.Component;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

@Component
public class InitialCommands {
    public SendMessage unknownUser(Update update, SendMessage message) {
        message.setText("Извините, мы не смогли найти Ваш номер в базе данных");
        return message;
    }

    //get phone number of unknown user
    public SendMessage getContact(Update update, SendMessage message) {
        message.setText("Ваш чат не активирован. Необходимо отправить контактные данные.");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        message.setReplyMarkup(keyboardMarkup);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        KeyboardButton keyboardButton = new KeyboardButton();
        keyboardButton.setText("Поделиться контактными данными");
        keyboardButton.setRequestContact(true);
        row.add(keyboardButton);
        keyboard.add(row);

        keyboardMarkup.setKeyboard(keyboard);

        return message;
    }
}

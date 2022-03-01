package kz.spt.tgbotplugin.component;

import kz.spt.lib.model.Cars;
import kz.spt.lib.model.Customer;
import kz.spt.lib.model.Parking;
import kz.spt.lib.model.telegram.TelegramBotTemporaryTime;
import kz.spt.lib.model.telegram.TelegramLastCommand;
import kz.spt.tgbotplugin.repository.TelegramBotLastCommandRepository;
import kz.spt.tgbotplugin.repository.TelegramBotTemporaryTimeRepository;
import kz.spt.tgbotplugin.service.RootServicesGetterService;
import kz.spt.whitelistplugin.model.AbstractWhitelist;
import kz.spt.whitelistplugin.model.Whitelist;
import kz.spt.whitelistplugin.model.WhitelistGroups;
import kz.spt.whitelistplugin.repository.WhitelistGroupsRepository;
import kz.spt.whitelistplugin.repository.WhitelistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class CommandsAdmin {
    private final WhitelistRepository whitelistRepository;
    private final WhitelistGroupsRepository whitelistGroupsRepository;
    private final RootServicesGetterService rootServicesGetterService;

    private final TelegramBotLastCommandRepository telegramBotLastCommandRepository;
    private final TelegramBotTemporaryTimeRepository telegramBotTemporaryTimeRepository;

    public SendMessage identifiedAdmin(Update update, SendMessage message, String chatId, String phoneNumber, int timeTemporary) {
        message.setText("Что Вы хотите сделать?");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        message.setReplyMarkup(keyboardMarkup);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add("Проверить машину в белом списке");
        keyboard.add(row);

        row = new KeyboardRow();
        row.add("Добавить машину в белый список");
        keyboard.add(row);

        row = new KeyboardRow();
        row.add("Задать длительность временного нахождения машин в белом списке (По умолчанию 4 часа)");
        keyboard.add(row);

        row = new KeyboardRow();
        row.add("Добавить гостевого номера, такси(удаляются через " + timeTemporary + " часа)");
        keyboard.add(row);

        row = new KeyboardRow();
        row.add("Удалить машину из белого списка");
        keyboard.add(row);

        keyboardMarkup.setKeyboard(keyboard);

        return message;
    }

    public SendMessage processCommandsAdmin(String command, String chatId, SendMessage message, Customer customer, TelegramLastCommand telegramLastCommand, int timeTemporary) {

        String lastCommand = "";
        if (telegramLastCommand != null) {
            lastCommand = telegramLastCommand.getLastCommand();
        }
        System.out.println(command);
        if (command.equals("Добавить гостевого номера, такси(удаляются через " + timeTemporary + " часа)") || command.equals("Добавить машину в белый список") || command.equals("Удалить машину из белого списка")) {
            TelegramLastCommand newTelegramLastCommand = new TelegramLastCommand();
            newTelegramLastCommand.setLastCommand(command);
            newTelegramLastCommand.setChatId(chatId);
            telegramBotLastCommandRepository.save(newTelegramLastCommand);
            message.setText("Напишите номер машины");
        }
        else if (command.equals("Проверить машину в белом списке")) {
            TelegramLastCommand newTelegramLastCommand = new TelegramLastCommand();
            newTelegramLastCommand.setLastCommand(command);
            newTelegramLastCommand.setChatId(chatId);
            telegramBotLastCommandRepository.save(newTelegramLastCommand);
            message.setText("Напишите номер машины без пробелов");
        }
        else if (command.equals("Задать длительность временного нахождения машин в белом списке (По умолчанию 4 часа)")) {
            String textMessage = "Напишите время в часах";
            ///////////////////////
            TelegramLastCommand newTelegramLastCommand = new TelegramLastCommand();
            newTelegramLastCommand.setLastCommand(command);
            newTelegramLastCommand.setChatId(chatId);
            telegramBotLastCommandRepository.save(newTelegramLastCommand);
            //////////////////////
            message.setText(textMessage);
        } else if (lastCommand.equals("Задать длительность временного нахождения машин в белом списке (По умолчанию 4 часа)")) {
            try {
                timeTemporary = Integer.parseInt(command);
                telegramBotLastCommandRepository.delete(telegramLastCommand);
                TelegramBotTemporaryTime telegramBotTemporaryTime = telegramBotTemporaryTimeRepository.getTelegramBotTemporaryTimeById(1L);
                telegramBotTemporaryTime.setTime_duration(timeTemporary);
                telegramBotTemporaryTimeRepository.save(telegramBotTemporaryTime);
                message.setText("Время успешно изменено");
            } catch (NumberFormatException nfe) {
                message.setText("Напишите числовое выражение без слов");
            }
        }
        else if (lastCommand.equals("Добавить гостевого номера, такси(удаляются через "+ timeTemporary + " часа)")) {
            Cars car = rootServicesGetterService.getCarsService().findByPlatenumber(command.toUpperCase());
            if (car == null) {
                car = new Cars();
                car.setPlatenumber(command.toUpperCase());
                car.setCustomer(customer);
                rootServicesGetterService.getCarsService().saveCars(car);
            }

            Whitelist whitelist = new Whitelist();
            whitelist.setCar(car);

            ///////////////////////////////////////
            WhitelistGroups newWhitelistGroups = new WhitelistGroups();
            newWhitelistGroups.setName("TelegramBotPeriod" + command.toUpperCase());
            List<Parking> list = (List<Parking>) rootServicesGetterService.getParkingService().listAllParking();
            newWhitelistGroups.setParking(list.get(list.size()-1));
            newWhitelistGroups.setType(AbstractWhitelist.Type.PERIOD);
            whitelist.setParking(list.get(0));
            Date date = new Date();
            Date endTime = new Date(date.getTime() + TimeUnit.HOURS.toMillis(timeTemporary));
            newWhitelistGroups.setAccess_start(date);
            newWhitelistGroups.setAccess_end(endTime);
            newWhitelistGroups = whitelistGroupsRepository.saveAndFlush(newWhitelistGroups);
            whitelist.setGroup(newWhitelistGroups);
            ///////////////////////////


            whitelistRepository.save(whitelist);
            message.setText("Номер успешно добавлен");
            telegramBotLastCommandRepository.delete(telegramLastCommand);
        } else if (lastCommand.equals("Добавить машину в белый список")) {
            Cars car = rootServicesGetterService.getCarsService().findByPlatenumber(command);
            if (car == null) {
                car = new Cars();
                car.setPlatenumber(command.toUpperCase());
                rootServicesGetterService.getCarsService().saveCars(car);
            }

            Whitelist whitelist = new Whitelist();
            whitelist.setCar(car);


            ////////////////////////////////
            WhitelistGroups whitelistGroups = whitelistGroupsRepository.getWhitelistGroupsByName("TelegramBotUnlimited");
            if (whitelistGroups == null) {
                WhitelistGroups newWhitelistGroups = new WhitelistGroups();
                newWhitelistGroups.setName("TelegramBotUnlimited");
                List<Parking> list = (List<Parking>) rootServicesGetterService.getParkingService().listAllParking();
                newWhitelistGroups.setParking(list.get(list.size()-1));
                whitelist.setParking(list.get(0));
                newWhitelistGroups.setType(AbstractWhitelist.Type.UNLIMITED);
                newWhitelistGroups = whitelistGroupsRepository.saveAndFlush(newWhitelistGroups);
                whitelist.setGroup(newWhitelistGroups);
            } else {
                whitelist.setGroup(whitelistGroups);
                List<Parking> list = (List<Parking>) rootServicesGetterService.getParkingService().listAllParking();
                whitelist.setParking(list.get(0));
            }
            ///////////////////////////////

            List<Parking> list = (List<Parking>) rootServicesGetterService.getParkingService().listAllParking();
            whitelist.setParking(list.get(0));
            whitelistRepository.save(whitelist);
            message.setText("Номер успешно добавлен");
            telegramBotLastCommandRepository.delete(telegramLastCommand);
        } else if (lastCommand.equals("Удалить машину из белого списка")) {
            Cars cars = rootServicesGetterService.getCarsService().findByPlatenumber(command);
           /* List<Whitelist> whitelistsCars = whitelistRepository.getWhitelistByCar(cars);
            for (Whitelist whitelist : whitelistsCars) {
                whitelistRepository.delete(whitelist);
            }*/
            message.setText("Машина успешно удалена");
            telegramBotLastCommandRepository.delete(telegramLastCommand);
        } else if (lastCommand.equals("Проверить машину в белом списке")) {
            Cars car = rootServicesGetterService.getCarsService().findByPlatenumber(command);
            boolean exist = false;
            if (exist) {
                message.setText("Данная машина существует в белом списке");
            } else {
                message.setText("Данной машины в белом списке нету");
            }
            telegramBotLastCommandRepository.delete(telegramLastCommand);
        }

        return message;
    }
}

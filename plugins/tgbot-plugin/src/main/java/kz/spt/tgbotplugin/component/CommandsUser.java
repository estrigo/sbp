package kz.spt.tgbotplugin.component;

import kz.spt.lib.model.Cars;
import kz.spt.lib.model.Customer;
import kz.spt.lib.model.Parking;
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
public class CommandsUser {
    private final WhitelistRepository whitelistRepository;
    private final WhitelistGroupsRepository whitelistGroupsRepository;
    private final RootServicesGetterService rootServicesGetterService;

    private final TelegramBotLastCommandRepository telegramBotLastCommandRepository;
    private final TelegramBotTemporaryTimeRepository telegramBotTemporaryTimeRepository;

    public SendMessage identifiedUser(Update update, SendMessage message, String chatId, String phoneNumber, int timeTemporary) {

        message.setText("Что Вы хотите сделать?");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        message.setReplyMarkup(keyboardMarkup);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add("Мои машины");
        keyboard.add(row);

        row = new KeyboardRow();
        row.add("Добавить гостевого номера, такси(удаляются через " + timeTemporary +" часа)");
        keyboard.add(row);

        keyboardMarkup.setKeyboard(keyboard);

        return message;
    }

    public SendMessage processCommands(String command, String chatId, SendMessage message, Customer customer, TelegramLastCommand telegramLastCommand, int timeTemporary) {
        String lastCommand = "";
        if (telegramLastCommand != null) {
            lastCommand = telegramLastCommand.getLastCommand();
        }
        System.out.println(command);
        String addTemporaryCar = "Добавить гостевого номера, такси(удаляются через " + timeTemporary +" часа)";
        if (command.equals(addTemporaryCar)) {
            TelegramLastCommand newTelegramLastCommand = new TelegramLastCommand();
            newTelegramLastCommand.setLastCommand(addTemporaryCar);
            newTelegramLastCommand.setChatId(chatId);
            telegramBotLastCommandRepository.save(newTelegramLastCommand);
            message.setText("Напишите номер машины");
        }
        else if (command.equals("Мои машины")) {
            String textMessage = "Машины:";
            String endTimeString = "";

            /*List<Cars> customerCars = carsService.findAllByCustomer(customer);
            for (Cars car : customerCars) {
                if (car.getDeleted() == false) {
                    List<Whitelist> currentCarList = whitelistRepository.getWhitelistByCar(car);
                    boolean isValid = false;
                    for (Whitelist whitelist : currentCarList) {
                        if (whitelist.getGroup() != null && ((whitelist.getGroup().getType() == AbstractWhitelist.Type.UNLIMITED)
                                || (whitelist.getGroup().getType() == AbstractWhitelist.Type.PERIOD && whitelist.getGroup().getAccess_end() != null && whitelist.getGroup().getAccess_end().after(new Date())))) {
                            isValid = true;
                            endTimeString = whitelist.getGroup().getAccess_end().toString();
                        }
                        if (isValid) {
                            textMessage += "\n" + car.getPlatenumber();
                        }
                    }

                }
            }*/
            message.setText(textMessage);
        }
        else if (lastCommand.equals(addTemporaryCar)) {
            //if car exist
            boolean isExist = false;
            Cars car = rootServicesGetterService.getCarsService().findByPlatenumber(command);
            /*if (car != null) {
                List<Whitelist> whitelists = whitelistRepository.getWhitelistByCar(car);
                if (!whitelists.isEmpty()) {
                    for (Whitelist whitelist : whitelists) {
                        if (whitelist.getGroup().getAccess_end() != null || whitelist.getGroup().getAccess_end().compareTo(new Date()) >= 0) {
                            isExist = true;
                            message.setText("Такая машина уже существует");
                            break;
                        }
                    }
                }
            }*/

            if (!isExist) {
                telegramBotLastCommandRepository.delete(telegramLastCommand);
                car = car == null ? new Cars() : car;
                car.setPlatenumber(command.toUpperCase());
                car.setCustomer(customer);
                Whitelist whitelist = new Whitelist();
                whitelist.setCar(car);

                ///////////////////////
                WhitelistGroups newWhitelistGroups = new WhitelistGroups();
                newWhitelistGroups.setName("TelegramBotPeriodByCustomer" + command.toUpperCase());
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
                ///////////////////////


                rootServicesGetterService.getCarsService().saveCars(car);
                whitelistRepository.save(whitelist);
                message.setText("Номер успешно добавлен");
            }
        }

        return message;
    }
}

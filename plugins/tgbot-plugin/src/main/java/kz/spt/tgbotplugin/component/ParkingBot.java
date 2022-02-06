package kz.spt.tgbotplugin.component;

import kz.spt.lib.model.Customer;
import kz.spt.lib.model.telegram.TelegramAdmin;
import kz.spt.lib.model.telegram.TelegramBotChatId;
import kz.spt.lib.model.telegram.TelegramBotTemporaryTime;
import kz.spt.lib.model.telegram.TelegramLastCommand;
import kz.spt.lib.service.CustomerService;
import kz.spt.tgbotplugin.repository.TelegramAdminRepository;
import kz.spt.tgbotplugin.repository.TelegramBotChatIdRepository;
import kz.spt.tgbotplugin.repository.TelegramBotLastCommandRepository;
import kz.spt.tgbotplugin.repository.TelegramBotTemporaryTimeRepository;
import kz.spt.tgbotplugin.service.RootServicesGetterService;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.annotation.PostConstruct;
import java.util.List;

@Component
@Log
public class ParkingBot extends TelegramLongPollingBot {
    @Autowired
    private CommandsUser commandsUser;

    @Autowired
    private CommandsAdmin commandsAdmin;

    @Autowired
    private InitialCommands initialCommands;

    @Autowired
    private RootServicesGetterService rootServicesGetterService;

    @Autowired
    private TelegramAdminRepository telegramAdminRepository;

    @Autowired
    private TelegramBotChatIdRepository telegramBotChatIdRepository;

    @Autowired
    private TelegramBotLastCommandRepository telegramBotLastCommandRepository;

    @Autowired
    private TelegramBotTemporaryTimeRepository telegramBotTemporaryTimeRepository;

    private int timeTemporary = 4;

   /* @PostConstruct
    public void register(){
        try{
            TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
            api.registerBot(new ParkingBot());
            log.info(this.getMe().toString());
        }catch (TelegramApiException e){
            e.printStackTrace();
        }
    }*/

    @Override
    public void onUpdateReceived(Update update) {
        log.info(update.getMessage().toString());
        //get chat ID
        String chatId = update.getMessage().getChatId().toString();
        //Create Send Message
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        //Command
        String command = update.getMessage().getText();
        //Phone Number
        String phoneNumber = "";

        //get user by telegram chat id
        TelegramBotChatId telegramBotChatId = telegramBotChatIdRepository.getTelegramBotChatIdByChatId(chatId);
        if (telegramBotChatId != null) {
            phoneNumber = telegramBotChatId.getPhoneNumber();
        } else if (update.getMessage().getContact() != null) {
            phoneNumber = update.getMessage().getContact().getPhoneNumber();
        }
        ///////////////////////////////

        if (phoneNumber.equals("")) { //if phone number is empty
            message = initialCommands.getContact(update, message);
            try {
                execute(message);
            } catch (TelegramApiException E) {
                E.printStackTrace();
            }
        }

        //get temporary time from DB
        TelegramBotTemporaryTime telegramBotTemporaryTime = telegramBotTemporaryTimeRepository.getTelegramBotTemporaryTimeById(1L);
        //get temporary time if any
        if (telegramBotTemporaryTime != null) {
            timeTemporary = telegramBotTemporaryTime.getTime_duration();
        }


        //get telegram admin by phone number if any
        TelegramAdmin telegramAdmin = telegramAdminRepository.getTelegramAdminByPhoneNumber(phoneNumber);
        //get customer by phone number if any
        //Customer customer = customerRepository.getCustomerByPhoneNumber(phoneNumber);
        Customer customer = null;

        //get phone number from admin or customer
        if (telegramAdmin != null) {
            phoneNumber = telegramAdmin.getPhoneNumber();
        } else if (customer != null) {
            phoneNumber = customer.getPhoneNumber();
        }

        //get last command by chat id
        List<TelegramLastCommand> lastCommands = telegramBotLastCommandRepository.getTelegramLastCommandByChatId(chatId);

        //if last command is not empty, get last from list
        TelegramLastCommand telegramLastCommand = null;
        if (!lastCommands.isEmpty()) {
            telegramLastCommand = lastCommands.get(lastCommands.size() - 1);
        }

        if (telegramAdmin == null && customer == null) { // if telegram user is not found from admin or customer db
            message = initialCommands.unknownUser(update, message);
        } else if (telegramAdmin != null) { //if user is admin
            //if user is using bot for the first time, save chat id in db
            if (telegramBotChatId == null) {
                telegramBotChatIdRepository.save(TelegramBotChatId.builder()
                        .phoneNumber(update.getMessage().getContact().getPhoneNumber())
                        .chatId(chatId)
                        .isAdmin(true)
                        .build());
            }
            //if last command is not null or current command is one of the options
            if (telegramLastCommand != null || command != null && (command.equals("Проверить машину в белом списке")
                    || command.equals("Добавить машину в белый список")
                    || command.equals("Добавить гостевого номера, такси(удаляются через " + timeTemporary + " часа)")
                    || command.equals("Удалить машину из белого списка")
                    || command.equals("Задать длительность временного нахождения машин в белом списке (По умолчанию 4 часа)")
            )) {
                message = commandsAdmin.processCommandsAdmin(command, chatId, message, customer, telegramLastCommand, timeTemporary);
            } else { //give initial commands
                message = commandsAdmin.identifiedAdmin(update, message, chatId, phoneNumber, timeTemporary);
            }
        } else if (customer != null) {
            if (telegramBotChatId == null) {
                telegramBotChatIdRepository.save(TelegramBotChatId.builder()
                        .phoneNumber(update.getMessage().getContact().getPhoneNumber())
                        .chatId(chatId)
                        .isAdmin(false)
                        .build());
            } else if (telegramLastCommand != null ||
                    (command != null && (command.equals("Добавить гостевого номера, такси(удаляются через " + timeTemporary + " часа)")
                            || command.equals("Мои машины")))) {
                message = commandsUser.processCommands(command, chatId, message, customer, telegramLastCommand, timeTemporary);
            } else {
                message = commandsUser.identifiedUser(update, message, chatId, phoneNumber, timeTemporary);
            }
        }


        try {
            execute(message);
        } catch (TelegramApiException E) {
            E.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        // TODO
        String username = "TestSptBot";
        return username;
    }

    @Override
    public String getBotToken() {
        String token = "5221895410:AAG1F8xzx0xYsF296qQJ11mSt_99FgHwWuc";
        return token;
    }
}

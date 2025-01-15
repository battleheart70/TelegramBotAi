package com.javarush.telegram;

import com.javarush.telegram.ChatGPTService;
import com.javarush.telegram.DialogMode;
import com.javarush.telegram.MultiSessionTelegramBot;
import com.javarush.telegram.UserInfo;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

public class TinderBoltApp extends MultiSessionTelegramBot {
    public static final String TELEGRAM_BOT_NAME;
    public static final String TELEGRAM_BOT_TOKEN;
    public static final String OPEN_AI_TOKEN;
    private ChatGPTService chatGPT = new ChatGPTService(OPEN_AI_TOKEN);
    private DialogMode currentDialogMode = null;
    private ArrayList<String> list = new ArrayList<>();
    private UserInfo me;
    private UserInfo friend;
    private int questionCount;

    static {
        Properties properties = new Properties();
        try (InputStream input = TinderBoltApp.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new RuntimeException("Sorry, unable to find config.properties");
            }
            properties.load(input);
        } catch (IOException ex) {
            throw new RuntimeException("Error loading config.properties", ex);
        }
        TELEGRAM_BOT_NAME = properties.getProperty("TELEGRAM_BOT_NAME");
        TELEGRAM_BOT_TOKEN = properties.getProperty("TELEGRAM_BOT_TOKEN");
        OPEN_AI_TOKEN = properties.getProperty("OPEN_AI_TOKEN");
    }

    public TinderBoltApp() {
        super(TELEGRAM_BOT_NAME, TELEGRAM_BOT_TOKEN);
    }

    @Override
    public void onUpdateEventReceived(Update update) {
        //TODO: основной функционал бота будем писать здесь
        String message = getMessageText();

        if (message.equals("/start")) {
            currentDialogMode = DialogMode.MAIN;
            sendPhotoMessage("main");
            sendTextMessage(loadMessage("main"));
            showMainMenu(
                    "Главное меню бота", "/start",
                    "Генерация Tinder-профиля", "/profile",
                    "Сообщение для знакомства", "/opener",
                    "Переписка от вашего имени", "/message",
                    "Переписка со звездами", "/date",
                    "Общение с чатом GPT", "/gpt");
            return;
        }
        if (message.equals("/gpt")){
            currentDialogMode = DialogMode.GPT;
            sendPhotoMessage("gpt");
            sendTextMessage(loadMessage("gpt"));
            return;
        }
        if (message.equals("/date")){
            currentDialogMode = DialogMode.DATE;
            sendPhotoMessage("date");
            sendTextButtonsMessage(loadMessage("date"),
                    "Арианна Гранде", "date_grande",
                    "Зендайя", "date_zendaya",
                    "Марго Робби", "date_robbie",
                    "Райан Гослинг", "date_gosling",
                    "Том Харди", "date_hardy");
            return;
        }
        if(message.equals("/message")){
            currentDialogMode = DialogMode.MESSAGE;
            sendPhotoMessage("message");
            sendTextButtonsMessage(loadMessage("message"), "Следующее сообщение","message_next",
                    "Пригласить на свидание", "message_date");
            return;
        }
        if(message.equals("/profile")){
            currentDialogMode = DialogMode.PROFILE;
            sendPhotoMessage("profile");
            me = new UserInfo();
            questionCount = 1;
            sendTextMessage("Сколько вам лет?");
            return;
        }
        if (message.equals("/opener")){
            currentDialogMode = DialogMode.OPENER;
            sendPhotoMessage("opener");
            sendTextMessage(loadMessage("opener"));
            friend = new UserInfo();
            questionCount = 1;
            sendTextMessage("Как зовут потенциального друга?");
            return;
        }


        if (currentDialogMode == DialogMode.GPT && !isMessageCommand()){
            Message msg = sendTextMessage("Подождите ChatGPT генерирует ответ...");
            String answer = chatGPT.sendMessage(loadPrompt("gpt"), message);
            updateTextMessage(msg, answer);
            return;
        }
        if (currentDialogMode == DialogMode.DATE && !isMessageCommand()){
            String query = getCallbackQueryButtonKey();
            if (query.startsWith("date_")){
                sendPhotoMessage(query);
                sendTextMessage("Отличный выбор! Можешь начинать переписку с выбранной звездой:");
                chatGPT.setPrompt(loadPrompt(query));
                return;
            }
            Message msg = sendTextMessage("Подождите ваш потенциальный партнер набирает текст...");
            String answer = chatGPT.addMessage(message);
            updateTextMessage(msg, answer);
            return;
        }
        if (currentDialogMode == DialogMode.MESSAGE && !isMessageCommand()){
            String query = getCallbackQueryButtonKey();
            if (query.startsWith("message_")){
                String userChatHistory = String.join("\n\n", list);
                Message msg = sendTextMessage("Подождите ChatGPT генерирует ответ...");
                String answer = chatGPT.sendMessage(loadPrompt(query), userChatHistory);
                updateTextMessage(msg, answer);
                return;
            }
            list.add(message);
            return;
        }
        if (currentDialogMode == DialogMode.PROFILE && !isMessageCommand()){
            switch (questionCount){
                case 1:
                    me.age = message;
                    questionCount = 2;
                    sendTextMessage("Кем вы работаете?");
                    return;
                case 2:
                    me.occupation = message;
                    questionCount = 3;
                    sendTextMessage("Какие у вас хобби?");
                    return;
                case 3:
                    me.hobby = message;
                    questionCount = 4;
                    sendTextMessage("Что вам НЕ нравится в людях?");
                    return;

                case 4:
                    me.annoys = message;
                    questionCount = 5;
                    sendTextMessage("Какие у вас цели знакомства?");
                    return;
                case 5:
                    me.goals = message;
                    String aboutMySelf = me.toString();
                    Message msg = sendTextMessage("Подождите ChatGPT генерирует ответ...");
                    String answer = chatGPT.sendMessage(loadPrompt("profile"), aboutMySelf);
                    updateTextMessage(msg, answer);
                    return;
            }
            return;
        }
        if (currentDialogMode == DialogMode.OPENER && !isMessageCommand()){
            switch (questionCount){
                case 1:
                    friend.name = message;
                    questionCount = 2;
                    sendTextMessage("Сколько лет вашему другу?");
                    return;
                case 2:
                    friend.age = message;
                    questionCount = 3;
                    sendTextMessage("Кем работает ваш друг?");
                    return;
                case 3:
                    friend.occupation = message;
                    questionCount = 4;
                    sendTextMessage("Какие у вашего друга хобби?");
                    return;
                case 4:
                    friend.hobby = message;
                    questionCount = 5;
                    sendTextMessage("Какие цели знакомства у вашего друга?");
                    return;
                case 5:
                    friend.goals = message;
                    String aboutFriend = friend.toString();
                    Message msg = sendTextMessage("Подождите ChatGPT генерирует ответ...");
                    String answer = chatGPT.sendMessage(loadPrompt("opener"), aboutFriend);
                    updateTextMessage(msg, answer);
                    return;
            }
            return;
        }

        sendTextMessage("Привет, нажми на /start для начала работы с ботом и просмотра меню.");

    }

    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(new TinderBoltApp());
    }
}

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
import java.util.Properties;

public class TinderBoltApp extends MultiSessionTelegramBot {
    public static final String TELEGRAM_BOT_NAME;
    public static final String TELEGRAM_BOT_TOKEN;
    public static final String OPEN_AI_TOKEN;
    private ChatGPTService chatGPT = new ChatGPTService(OPEN_AI_TOKEN);
    private DialogMode currentDialogMode = null;

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
            String text = loadMessage("gpt");
            sendTextMessage(text);
            return;
        }

        if (currentDialogMode == DialogMode.GPT){
            String prompt = loadPrompt("gpt");
            String answer = chatGPT.sendMessage(prompt, message);
            sendTextMessage(answer);
            return;
        }

        sendTextMessage("*Привет, ты мой бот!*");
        sendTextMessage("_Привет, ты мой бот!_");

        sendTextMessage("Ты написал: " + message);
        sendTextButtonsMessage("Выбери режим работы", "Start", "/start",
                "chatGPT", "/gpt");


    }

    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(new TinderBoltApp());
    }
}

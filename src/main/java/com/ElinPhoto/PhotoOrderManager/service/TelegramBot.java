package com.ElinPhoto.PhotoOrderManager.service;

import com.ElinPhoto.PhotoOrderManager.config.BotConfig;
import com.ElinPhoto.PhotoOrderManager.model.Order;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@Data
@PropertySource("application.properties")
@PropertySource("message.properties")

public class TelegramBot extends TelegramLongPollingBot {
    @Autowired
    private PriceCounter priceCounter;
    final BotConfig config;
    @Autowired
    private Order order;
    @Autowired
    private MailService mailService;
//    @Value("${price.text}")
//    String PRICE_TEXT;
    static final String PRICE_TEXT = "✅АКЦИЯ «ВСЕ ФОТО НА ПОЧТУ»\n" +
            "                      1200рублей \n" +
            " ( не будет бесплатной напечатанной фотографии, как при заказе в детском саду)\n" +
            "\n" +
            "✅Электронные фотографии поштучно \n" +
            "          (продажа от 2х фотографий одного ребенка)\n" +
            "\n" +
            "                     1 фото - 200рублей\n" +
            "*Фотографии отправляются в формате JPEG \n" +
            "\n" +
            "\n" +
            "                        ‼️ВНИМАНИЕ \n" +
            "НОМЕРА ФОТОГРАФИЙ ПИШИТЕ ПРАВИЛЬНО. \n" +
            "В СЛУЧАЕ, ЕСЛИ ВЫ ОШИБЛИСЬ НОМЕРОМ И ВАМ БЫЛИ ОТПРАВЛЕНЫ ЧУЖИЕ ФОТОГРАФИИ РЕБЁНКА, ОТВЕТСТВЕННОСТЬ ЗА ЭТО МЫ НЕ НЕСЁМ \n" +
            "\n" +
            "                 Печать фотографий \uD83D\uDDBC\n" +
            "\n" +
            "                \uD83D\uDD1815х20 - 300рублей\n" +
            "                \uD83D\uDD1820х30 - 400рублей \n" +
            "                \uD83D\uDD1830х40 - 550 рублей\n" +
            "\n" +
            "Печать фотографий осуществляется в профессиональной фотолаборатории Москвы и гарантирует сохранение качества и цвета ваших снимков на долгие годы.";
    static final String ERROR_TEXT = "Error occurred: ";
    static final String YES_BUTTON = "YES_BUTTON";
    static final String NO_BUTTON = "NO_BUTTON";
    // конструктор
    public TelegramBot(BotConfig config) {
        this.config = config;

        List<BotCommand> listOfCommands = new ArrayList(); // Класс BotCommand определен в телеграм библиотеке
        listOfCommands.add(new BotCommand("/start", "Начальная страница")); // конструктор принимает 2 аргумента 1 название команды 2 описание

        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bot`s commands list: " + e);
        }

    }
    @Override
    public String getBotUsername() { // вернет имя бота
        return config.getBotName();
    }
    @Override
    public String getBotToken() { // вернет ключ для бота
        return config.getToken();
    }
    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
                switch (messageText) {
                    case "/start":
                    case "Назад":
                        startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                        break;
                    case "Цены и правила\uD83D\uDCC3\uD83D\uDCB0":
                        prepareAndSendMessage(chatId, PRICE_TEXT);
                        break;
                    case "Заказать фото\uD83E\uDD17":
                        photoOrder(chatId);
                        break;
                    case "Проблемы с заказом\uD83E\uDD2F":
                        invalidOrderCommandReceived(chatId);
                        break;
                    case "Заказ не получен\uD83C\uDD98":
                        whereIsMyOrder(chatId);
                        break;
                    case "Получены не все фото‼":
                        whereIsMyPhotos(chatId);
                        break;
                    case "Заказать все фото\uD83D\uDE0D":
                        allPhotoOrder(chatId);
                        break;
                    case "Заказать поштучно☺":
                        choosePhotosToOrder(chatId);
                        break;
                    case "Частые вопросы\uD83E\uDD13":
                        frequentlyAskedQuestions(chatId);
                        break;
                    case "Оплатить✅":
                        payCommandReceived(chatId);
                        break;
                    case "Пришли фотографии не моего ребенка\uD83D\uDE31":
                        thisNotMyChildPhotos(chatId);
                        break;
                }
            }
            if(update.hasMessage() && update.getMessage().hasPhoto()) {
                Long chatId = update.getMessage().getChatId();
                String message = "Теперь отправь боту сообщение,\nв котором укажи " +
                        "все данные\nнеобходимые для заказа \uD83D\uDC47\n" +
                        "Бот не увидит заказ без электронной почты.";
                order.setOrderFlag("Новый заказ");

                List<PhotoSize> photoSizes = update.getMessage().getPhoto();
                getFileUrl(update.getMessage().getPhoto().get(photoSizes.size() - 2).getFileId());
                prepareAndSendMessage(chatId, message);

            } else if (update.hasMessage() && update.getMessage().hasDocument()) {
                Long chatId = update.getMessage().getChatId();
                String message = "Теперь отправь боту сообщение, в котором укажи " +
                        "все данные необходимые для заказа \uD83D\uDC47 \n" +
                        "Бот не увидит заказ без электронной почты.";
                Document document = update.getMessage().getDocument();
                getFileUrl(document.getFileId());
                prepareAndSendMessage(chatId, message);
                order.setOrderFlag("Новый заказ");

            } else if (update.hasMessage() && update.getMessage().hasText()) {
                StringBuilder builder = new StringBuilder();
                char[] chars = update.getMessage().getText().toCharArray();
                for (char ch : chars) {
                    if(ch == '@') {
                        builder.append(update.getMessage().getText())
                               .append("\nНомер чата ")
                               .append(update.getMessage().getChatId());
                        order.setOrderText(builder.toString());
                        System.out.println(order.getOrderText());
                        orderConfirm(update.getMessage().getChatId());
                        break;

                    }

            }
        } else if(update.hasCallbackQuery()) {
                String callBackData = update.getCallbackQuery().getData();
                long messageId = update.getCallbackQuery().getMessage().getMessageId();
                long chatId = update.getCallbackQuery().getMessage().getChatId();
                // проверяем какой строке соответствует
                if (callBackData.equals(YES_BUTTON)) {
                    if (!order.getOrderFlag().equals("null")) {
                        String text = "Ваш заказ принят\uD83E\uDD17\n" +
                                "Ожидайте ваши фотографии на почте в течении 24 часов⏳\n" +
                                "Проверяйте папку Спам\uD83D\uDCD1";
                        sendMessage(text, chatId, messageId);
                        prepareAndSendMessage(config.getOwnerId(), "Новая заявка! Обращение - " + order.getOrderFlag());
                        mailService.sendEmail(order);

                    } else if (order.getOrderFlag().equals("null")) {
                        String text = "Не могу Вас понять! Выберите нужный раздел или перейдите на начальную страницу в пукте \"Меню\".";
                        sendMessage(text, chatId, messageId);

                    }
                } else if (callBackData.equals(NO_BUTTON)) {
                    String text = "Заявка отменена.";
                    sendMessage(text, chatId, messageId);
                    order.setOrderFlag(null);
                    order.setOrderText(null);
                    order.setOrderConfirmURL(null);
                }
            }
        if(update.hasMessage() && update.getMessage().hasText()) {
            long chatId = update.getMessage().getChatId();
            String message = update.getMessage().getText();
            try {
                if(Integer.valueOf(message.trim()) > 0) {
                    SendMessage sendMessage =  new SendMessage();
                    sendMessage.setText( priceCounter.selectedPhotosPrise(Integer.parseInt(message)));
                    sendMessage.setChatId(String.valueOf(chatId));
                    sendMessage(sendMessage);
                }
            }catch (NumberFormatException e) {

            }
        }
        if(update.hasMessage() && update.getMessage().getChatId().equals(config.getOwnerId())) {
            String target = update.getMessage().getText();
            String[] split = target.split(" ", 2);
            SendMessage sendMessage = new SendMessage(split[0], split[1]);
            sendMessage(sendMessage);

        }
    }
    private void getFileUrl(String fileId) throws IOException {

        URL url = new URL("https://api.telegram.org/bot"+this.getBotToken()+"/getFile?file_id="+fileId);
        BufferedReader in = new BufferedReader(new InputStreamReader( url.openStream()));
        String res = in.readLine();
        JSONObject jResult = new JSONObject(res);
        JSONObject path = jResult.getJSONObject("result");
        String file_path = path.getString("file_path");
        URL download = new URL("https://api.telegram.org/file/bot" + this.getBotToken() + "/" + file_path);

        order.setOrderConfirmURL(String.valueOf(download));
    }
    private void frequentlyAskedQuestions(long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText("ЧАСТО ЗАДАВАЕМЫЕ ВОПРОСЫ:\n" +
                "\n" +
                " 1. <b><i>Когда будут готовы электронные фотографии?</i></b>\n" +
                "Электронные фотографии обычно отправляются через 10 дней после того как вам в саду отправляют пробные экземпляры.  \n" +
                " 2. <b><i>Когда будут готовы напечатанные фотографии?</i></b>\n" +
                "Напечатанные фотографии изготавливаются в Москве, поэтому их ждать чуть дольше электронных. 20-25 дней с момента получения пробных\n" +
                "3. <b><i>Фотографии не грузятся/ не открываются/ открываются в маленьком формате\n</i></b>" +
                "Это происходит из за того, что у тебя плохо ловит интернет. \n" +
                "Всё, других причин нет. \n" +
                "Мы отправляем письма и каждое проверяем в ручную!\n" +
                "Заказчикам уходят 100% качественные фотографии и надежные письма.\n" +
                "\n" +
                "<b><i>Варианты решения проблемы\n</i></b>" +
                " 1. Wi-fi - перезагрузи приложение, (если открываешь письмо с телефона) и подключи вай-фай. \n" +
                " 2. Открой письмо с компьютера и скачай.\n" +
                "\n" +
                "\n" +
                " 4. <b><i>Пришли не все фотографии! Я заказывал/ла другое количество!</i></b>\n" +
                "Скорее всего ты не прочитал тему письма. Там указано, что в письме все фото и они прикреплены ссылками. \n" +
                "Чтобы их обнаружить нужно пролистать письмо вниз до конца. \n" +
                "Либо нажать «скачать все» и уже в своих загрузках обнаружить полный заказ.\n" +
                "Если вдруг после всех манипуляций фотографии все-таки не найдены, напиши нам об этом на почте в ответном письме. Так мы скорее решим проблему.");
        sendMessage.setParseMode(ParseMode.HTML);


        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage(sendMessage);
    }

    private void whereIsMyOrder(long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText("Если ты заказал и оплатил фотографии, но на твою почту письмо не пришло:\n" +
                " 1. Проверь папку Спам и поищи письмо от электронного адреса Linok1@mail.ru\n" +
                " 2. Проверь корзину\n" +
                " 3. Если письма нет нигде, то отправь Боту сообщение, в котором укажи:\n" +
                            "- Номер детского сада\n" +
                            "- Группу\n" +
                            "- Фамилию ребенка\n" +
                            "- Твою электронную почту\n" +
                            "- Твой мобильный номер" +
                "\n" +
                "Ожидай, в течении 24 часов твоя проблема будет решена. ");
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage(sendMessage);

        order.setOrderFlag("Заказ не получен.");
    }
    @SneakyThrows
    private void whereIsMyPhotos(Long chatId) {

        SendMessage sendMessage = new SendMessage();
        sendMessage.setText("Если пришло письмо, но фотографии не все:\n" +
                "\n" +
                "Если ты получил письмо и в нем не хватает фотографий, то убедись, что ты пролистал письмо вниз до конца и открыл прикрепленные ссылки! \n" +
                "Объем фотографий большой и почта часть фотографий прикрепляет к письму в виде ссылок.\n" +
                "В каждом телефоне и в каждой почте это выглядит по разному. " +
                "Обрати внимание, что фотографии по ссылке хранятся несколько месяцев! Скачай их заблаговременно ☝\uD83C\uDFFB\n" +
                "\n" +
                "\n" +
                "\n" +
                "Если все же фотографий не хватает, то прошу тебя в ответном\n" +
                "письме (с которого ты получил фото на почте) отправить\n" +
                "сообщение с данной информацией. Так мы быстрее решим эту проблему.");

        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage(sendMessage);

    }

    private void startCommandReceived(long chatId, String name) {

        String textToSend = "Чем я могу Вам помочь?\uD83E\uDD17";

        List<KeyboardRow> keyboardRows = new ArrayList<>();
        keyboardRows.add(createRow(new String[]{"Цены и правила\uD83D\uDCC3\uD83D\uDCB0", "Заказать фото\uD83E\uDD17"}));
        keyboardRows.add(createRow(new String[]{"Проблемы с заказом\uD83E\uDD2F", "Частые вопросы\uD83E\uDD13"}));
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setKeyboard(keyboardRows);
        keyboardMarkup.setResizeKeyboard(true);

        sendMessage(chatId, textToSend,keyboardMarkup);
        log.info("Replied to user " + name);

    }
    // Ответ на проблемы с заказом
    private void invalidOrderCommandReceived(long chatId) {
        String textToSend = "Что именно у Вас произошло?";
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        // создаем ряд кнопок, будет сформирован по порядку добавления
        keyboardRows.add(createRow(new String[]{"Заказ не получен\uD83C\uDD98", "Получены не все фото‼"}));
        keyboardRows.add(createRow(new String[]{"Пришли фотографии не моего ребенка\uD83D\uDE31"}));
        keyboardRows.add(createRow(new String[]{"Назад"}));
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setKeyboard(keyboardRows);// к высылаемому сообщению прикрепляем клавиатуру sendMessage.setReplyMarkup(keyboardMarkup);
        keyboardMarkup.setResizeKeyboard(true);

        sendMessage(chatId, textToSend,keyboardMarkup);
    }
    private void thisNotMyChildPhotos(Long chatID) {
        String txtToSend = "Если тебе пришли фотографии чужого ребенка, то скорее всего это произошло потому что в заказе был написан не верный номер. \n" +
                "Другая причина, это ошибка нашего сотрудника и человеческий фактор. \n" +
                "В любом случае это не страшно и исправимо! \n" +
                "☑️Напиши номер садика\n" +
                "☑️Группу \n" +
                "☑️Фамилию \n" +
                "☑️ Продублируй номер фото\n" +
                "В течении дня письмо снова будет отправлено.\n" +
                "\n" +
                "\n" +
                "*Если это был заказ на все фото + 1 бесплатная по акции, а номер в бланке тобой был указан не верно - повторные электронные фотографии  будут отправлены, но напечатанной версии не будет.";
        sendMessage(new SendMessage(String.valueOf(chatID), txtToSend));
        order.setOrderFlag("Пришли фото не моего ребенка");
    }

    //Заказ фото- должен содержать два варианта выставления оплаты
    // Заказ всех фото имеет фиксированную цену- впишем как константу
    // заказ фото выборочно должен иметь счетчик для расчета цены, начиная от минимального заказа (2 фото - 400 руб)
    private void photoOrder(long chatId) {
        String textToSend = "Выберите вариант заказа";
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        keyboardRows.add(createRow(new String[]{"Заказать все фото\uD83D\uDE0D", "Заказать поштучно☺"}));
        keyboardRows.add(createRow(new String[]{"Назад"}));
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setKeyboard(keyboardRows);// к высылаемому сообщению прикрепляем клавиатуру sendMessage.setReplyMarkup(keyboardMarkup);
        keyboardMarkup.setResizeKeyboard(true);

        sendMessage(chatId, textToSend,keyboardMarkup);
    }

    // Выборочный заказ фото
    private void allPhotoOrder(Long chatId) {
        String textToSend = "Ты можешь заказать сразу все фотографии в формате JPEG, на\nсвою электронную почту.\n" +
                "Стоимость этого заказа составляет 1200р\n" +
                "Почта ICloud.com не принимает наши большие письма, не " +
                "используй её при заказе.\n" +
                "‼Обращаем так же твое внимание\n" +
                "Бесплатная фотография в подарок через оплату по этому боту не изготавливается!\n" +
                "Бесплатная фотография печатается только если ты заказал и\n" +
                "оплатил фото в детском саду!\n" +
                "Если ты не передумал и согласен с этим условием жми " +
                "«Оплатить» и следуй инструкции\uD83D\uDC47\uD83C\uDFFC";

        List<KeyboardRow> keyboardRows = new ArrayList<>();

        keyboardRows.add(createRow(new String[]{"Оплатить✅"}));
        keyboardRows.add(createRow(new String[] {"Назад"}));
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setKeyboard(keyboardRows);
        keyboardMarkup.setResizeKeyboard(true);

        sendMessage(chatId, textToSend,keyboardMarkup);

    }
    private void payCommandReceived(Long chatId) {
        SendMessage sendMessage =new SendMessage(String.valueOf(chatId), priceCounter.allPhotosPrise());
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        keyboardRows.add(createRow(new String[]{"Назад"}));
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setKeyboard(keyboardRows);
        keyboardMarkup.setResizeKeyboard(true);

        sendMessage.setReplyMarkup(keyboardMarkup);
        sendMessage(sendMessage);
        order.setOrderFlag("Новый заказ");
    }
    private void choosePhotosToOrder(Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText("Фотографии отправляются на почту при заказе от 2х фотографий одного ребенка(400 рублей)! \n" +
                "Стоимость 1 фотографии - 200 рублей\n" +
                "Формат электронных фотографий JPEG \n" +
                "Напиши сколько фото хочешь получить \uD83D\uDC47\uD83C\uDFFC ( 2 или более ?)");
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage(sendMessage);

    }
    private void orderConfirm(long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText("Пожалуйста, проверьте правильность заполнения заявки\n\nОтправить?");

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsLines = new ArrayList<>();

        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        var yesButton = new InlineKeyboardButton();
        yesButton.setText("ДА ✅");
        yesButton.setCallbackData(YES_BUTTON);

        var noButton = new InlineKeyboardButton();
        noButton.setText("НЕТ❌");
        noButton.setCallbackData(NO_BUTTON);

        rowInline.add(yesButton);
        rowInline.add(noButton);

        rowsLines.add(rowInline);
        markupInLine.setKeyboard(rowsLines);

        sendMessage.setReplyMarkup(markupInLine);

        sendMessage(sendMessage);
    }
    private void sendMessage(long chatId, String textToSend, ReplyKeyboardMarkup keyboardMarkup) {

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(textToSend);

        sendMessage.setReplyMarkup(keyboardMarkup);
        sendMessage(sendMessage);
    }
    private void sendMessage(SendMessage sendMessage) { // метод исполняющий посылку сообщения
        try {
            execute(sendMessage);
        }
        catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }
    private void sendMessage(String text, long chatId, long messageId) { // метод исполняющий посылку сообщения

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(String.valueOf(chatId));
        editMessageText.setText(text);
        editMessageText.setMessageId((int)messageId);
        try {
            execute(editMessageText);
        }
        catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }
    private void prepareAndSendMessage(long chatId, String textToSend) {

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(textToSend);
        sendMessage(sendMessage);
    }
    // вернуть готовый ряд клавиш
    public KeyboardRow createRow(String[] keyRow) {
        KeyboardRow row = new KeyboardRow();
        for (String key : keyRow) {
            row.add(key);
        }
        return row;
    }
}

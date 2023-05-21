package com.ElinPhoto.PhotoOrderManager.service;


import org.springframework.stereotype.Component;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.Properties;

@Component
public class MailService {
    public void sendEmail(Order order) throws MessagingException {
        StringBuilder buildMyMessage =new StringBuilder();
        buildMyMessage.append(order.getOrderText());

        if (order.getOrderConfirmURL() != null)    {
            buildMyMessage.append("\nФайл подтверждения ")
                    .append(order.getOrderConfirmURL());
        }

        String user = "";
        String password = "";

        String to = "";
        String smtpHost = "smtp.yandex.ru";
        Integer port = 465;

        Properties prop = new Properties();
        prop.put("mail.smtp.host", smtpHost);
        prop.put("mail.smtp.ssl.enable", "true");
        prop.put("mail.smtp.port", port);
        prop.put("mail.smtp.auth", "true");

        Session session = Session.getInstance(prop, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, password);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(user));

        InternetAddress[] addresses = {new InternetAddress(to)};

        message.setRecipients(Message.RecipientType.TO, addresses);
        message.setSubject("Заявка! " + order.getOrderFlag());
        message.setSentDate(new Date());
        message.setText(buildMyMessage.toString());

        Transport.send(message);

        order.setOrderFlag(null);
        order.setOrderText(null);
        order.setOrderConfirmURL(null);
    }

}

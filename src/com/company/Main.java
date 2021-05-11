package com.company;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws MessagingException { // с yandex smtp была возможность тестить, при желании можно настроить свой сервис
        Encryption encryption = Encryption.getDefault("key", "salt", new byte[16]);
        Scanner in = new Scanner(System.in);
        char state = in.nextLine().charAt(0);
        if (state=='s') { //sender

            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(javax.mail.Session.class.getClassLoader());

                Properties properties = new Properties();
                properties.put("mail.smtp.host", "smtp.yandex.ru");
                properties.put("mail.smtp.auth", "true");
                properties.put("mail.smtp.socketFactory.port", "465");
                properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");

                Session session = Session.getDefaultInstance(properties,
                        new Authenticator() {
                            @Override
                            protected PasswordAuthentication getPasswordAuthentication() {
                                return new PasswordAuthentication("user", "password");
                            }
                        });
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress("from"));
                message.setRecipient(Message.RecipientType.TO, new InternetAddress("to"));
                message.setSubject("тема письма");
                message.setText(encryption.encryptOrNull("text"));
                Transport.send(message);
            } finally {
                Thread.currentThread().setContextClassLoader(loader);
            }
        } else if (state=='r'){ //receiver
            Properties properties = new Properties();

            properties.setProperty("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");

            Session session = Session.getDefaultInstance(properties);

            Store store = null;
            try {

                store = session.getStore("imap");
                store.connect("imap.yandex.ru", 993, "user", "pass");
                Folder inbox = null;
                try {

                    inbox = store.getFolder("INBOX");
                    inbox.open(Folder.READ_ONLY);

                    int count = inbox.getMessageCount();

                    Message[] messages = inbox.getMessages(1, count);

                    for (Message message : messages) {
                        String from = ((InternetAddress) message.getFrom()[0]).getAddress();
                        System.out.println("FROM: " + from);
                        System.out.println("SUBJECT: " + message.getSubject());
                        System.out.println("TEXT: "+encryption.decryptOrNull(String.valueOf(message)));
                    }
                } finally {
                    if (inbox != null) {
                        inbox.close(false);
                    }
                }
            } finally {
                if (store != null) {
                    store.close();
                }
            }
        }
    }
}
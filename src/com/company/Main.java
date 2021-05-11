package com.company;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws MessagingException { // с yandex smtp была возможность тестить, при желании можно настроить свой сервис
        Scanner in = new Scanner(System.in);
        System.out.println("Enter the encryption key:");
        String key=in.nextLine();
        System.out.println("Enter the encryption salt:");
        String salt=in.nextLine();
        Encryption encryption = Encryption.getDefault(key, salt, new byte[16]);
        System.out.println("Enter the email address of your work account:");
        String name=in.nextLine();
        System.out.println("Enter the special SMTP account token issued by yandex:");
        String pass=in.nextLine();
        System.out.println("Enter the operating mode('s' - sending,'r' - receive):");
        char state = in.nextLine().charAt(0);
        if (state=='s') { //sender
            System.out.printf("Enter the email address you want to send from:");
            String email=in.nextLine();
            System.out.printf("Enter where:");
            String email2=in.nextLine();
            System.out.printf("Enter the subject of the email:");
            String sbg=in.nextLine();
            System.out.printf("Enter the message text:");
            String text=in.nextLine();
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
                                return new PasswordAuthentication(name, pass);
                            }
                        });
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(email));
                message.setRecipient(Message.RecipientType.TO, new InternetAddress(email2));
                message.setSubject(sbg);
                message.setText(encryption.encryptOrNull(text));
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
                store.connect("imap.yandex.ru", 993, name, pass);
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
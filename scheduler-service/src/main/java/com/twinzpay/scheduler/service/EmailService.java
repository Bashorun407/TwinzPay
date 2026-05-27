package com.twinzpay.scheduler.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPaymentWarning(String toEmail, String timeRemaining, String amount, String account) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("lytwind25@gmail.com");
        message.setTo(toEmail);
        message.setSubject("Upcoming Payment Reminder: " + timeRemaining);

        String body = String.format(
                "Hello,\n\n" +
                        "This is a reminder that your scheduled automated payment of NGN %s for account/meter %s " +
                        "will be processed in exactly %s.\n\n" +
                        "If you wish to cancel or modify this payment, please log into your TwinzPay dashboard immediately.\n\n" +
                        "Thank you,\nTwinzPay Automation Team",
                amount, account, timeRemaining
        );

        message.setText(body);
        mailSender.send(message);
    }
}

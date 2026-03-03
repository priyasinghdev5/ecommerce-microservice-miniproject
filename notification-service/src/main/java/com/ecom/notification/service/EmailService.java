package com.ecom.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    /**
     * Send HTML email using Thymeleaf template.
     *
     * @param to        recipient email
     * @param subject   email subject
     * @param template  Thymeleaf template name (without .html)
     * @param variables template variables
     * @return message ID from SMTP server
     */
    public String sendHtmlEmail(String to, String subject, String template,
                                Map<String, Object> variables) throws MessagingException {
        Context ctx = new Context();
        variables.forEach(ctx::setVariable);
        String htmlBody = templateEngine.process(template, ctx);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);

        mailSender.send(message);
        log.info("Email sent to={} subject={}", to, subject);
        return message.getMessageID();
    }

    /**
     * Send plain text email (for simple notifications).
     */
    public void sendPlainEmail(String to, String subject, String body) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body, false);
        mailSender.send(message);
        log.info("Plain email sent to={}", to);
    }
}

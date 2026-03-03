package com.ecom.notification.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * SMS service via Twilio.
 * In dev/test mode (when accountSid is "dev"), logs the SMS instead of sending.
 */
@Slf4j
@Service
public class SmsService {

    @Value("${twilio.account-sid:dev}")
    private String accountSid;

    @Value("${twilio.auth-token:dev}")
    private String authToken;

    @Value("${twilio.from-number:+15555555555}")
    private String fromNumber;

    @PostConstruct
    public void init() {
        if (!"dev".equals(accountSid)) {
            Twilio.init(accountSid, authToken);
            log.info("Twilio initialized with accountSid={}", accountSid);
        } else {
            log.info("Twilio running in DEV mode — SMS will be logged only");
        }
    }

    /**
     * Send SMS via Twilio.
     * Returns the Twilio message SID (reference ID).
     */
    public String sendSms(String toNumber, String body) {
        if ("dev".equals(accountSid)) {
            // Dev mode — just log it
            log.info("[DEV SMS] to={} body={}", toNumber, body);
            return "DEV_SID_" + System.currentTimeMillis();
        }

        Message message = Message.creator(
                new PhoneNumber(toNumber),
                new PhoneNumber(fromNumber),
                body
        ).create();

        log.info("SMS sent: to={} sid={}", toNumber, message.getSid());
        return message.getSid();
    }
}

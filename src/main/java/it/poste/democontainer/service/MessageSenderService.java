/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.poste.democontainer.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 *
 * @author GIANGR40
 */
@Service
@Slf4j
public class MessageSenderService {

    private StreamBridge streamBridge;

    public MessageSenderService(StreamBridge streambridge) {
        streamBridge = streambridge;
    }

    @Retryable(value = Throwable.class,
            maxAttempts = 3, backoff = @Backoff(delay = 5_000, multiplier = 1))
    public void sendMessage(String message) {

        log.info("Sending new message: {}", message);
        //try {
        //    Thread.sleep(50_000);
        //} catch (InterruptedException ex) {
        //}

        if (streamBridge.send("supplier-out-0", message) == false) {
            log.error("####### Error while sending....{}", message);
            throw new RuntimeException("error while sending message:" + message);
        } // if
        else {
            log.info("message: {} has been sent", message);
        }

    }

}

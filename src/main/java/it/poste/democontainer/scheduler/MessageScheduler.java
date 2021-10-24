/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.poste.democontainer.scheduler;

import it.poste.democontainer.service.MessageSenderService;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author GIANGR40
 */
@Service
@Slf4j
public class MessageScheduler {

    private MessageSenderService messageSenderService;
    private AtomicInteger messageCounter;

    public MessageScheduler(MessageSenderService messagesenderservice) {
        messageSenderService = messagesenderservice;
        messageCounter
                = new AtomicInteger(0);
    }

    @Scheduled(fixedDelayString = "${app.schedule.fixedDelay}",
            initialDelayString = "${app.schedule.initialDelay}")
    public void sendScheduledMessage() {
        log.info("start scheduled task...");
        String message = "Hello World! #" + messageCounter.incrementAndGet();
        messageSenderService.sendMessage(message);
        log.info("scheduled task...handled {}", message);
    }
    
}

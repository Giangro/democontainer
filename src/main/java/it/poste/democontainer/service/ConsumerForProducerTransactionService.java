/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.poste.democontainer.service;

import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author GIANGR40
 */
@Service
@Slf4j
public class ConsumerForProducerTransactionService {

    private StreamBridge streamBridge;        
    private Boolean raiseException;
    
    public ConsumerForProducerTransactionService(StreamBridge streambridge) {
        streamBridge = streambridge;
        raiseException = true;
    }
    
    @Bean
    public Consumer<String> process() {
        return msg->run(msg);
    }
    
    @Transactional
    public void run(String msg) {
        log.info("Received event={}", msg);
        log.info("!!! STOP KAFKA in 35 sec. FOR SIMULATING EXCEPTION !!!");
        try {
            Thread.sleep(35_000);
        } catch (InterruptedException ex) {
        }
        log.info("Message handled={}", msg);        
        streamBridge.send("process-out-0", msg.toUpperCase());
        if ("HELLO WORLD! #2".equals(msg.toUpperCase()) && raiseException == true) {
                raiseException = true;
                log.error("throw run time exception for {}", msg.toUpperCase());
                throw new RuntimeException("!!!!!! Simulate exception for:" + msg.toUpperCase());
       } // if
    }

}

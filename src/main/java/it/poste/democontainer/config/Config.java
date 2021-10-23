/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.poste.democontainer.config;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.springframework.cloud.stream.binder.RequeueCurrentMessageException;
import org.springframework.cloud.stream.binder.kafka.KafkaBindingRebalanceListener;
import org.springframework.cloud.stream.config.ListenerContainerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.kafka.listener.DefaultAfterRollbackProcessor;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.backoff.FixedBackOff;

/**
 *
 * @author GIANGR40
 */
@Configuration
@EnableTransactionManagement
@Slf4j
public class Config {

    private AtomicInteger messageCounter
            = new AtomicInteger(0);

    @Bean
    KafkaBindingRebalanceListener kafkaBindingRebalanceListener() {
        return new KafkaBindingRebalanceListener() {
            @Override
            public void onPartitionsRevokedBeforeCommit(String bindingName, Consumer<?, ?> consumer, Collection<TopicPartition> partitions) {
                KafkaBindingRebalanceListener.super.onPartitionsRevokedBeforeCommit(bindingName, consumer, partitions); //To change body of generated methods, choose Tools | Templates.                                
            }

            @Override
            public void onPartitionsRevokedAfterCommit(String bindingName, Consumer<?, ?> consumer, Collection<TopicPartition> partitions) {
                KafkaBindingRebalanceListener.super.onPartitionsRevokedAfterCommit(bindingName, consumer, partitions); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void onPartitionsAssigned(String bindingName, Consumer<?, ?> consumer, Collection<TopicPartition> partitions, boolean initial) {
                log.info("partition assigned = {}", partitions.toString());
                KafkaBindingRebalanceListener.super.onPartitionsAssigned(bindingName, consumer, partitions, initial); //To change body of generated methods, choose Tools | Templates.
            }

        };
    }

    @Bean
    public ListenerContainerCustomizer<AbstractMessageListenerContainer<byte[], byte[]>> customizer() {
        // Disable retry in the AfterRollbackProcessor
        return (container, destination, group) -> container.setAfterRollbackProcessor(
                new DefaultAfterRollbackProcessor<byte[], byte[]>(
                        (record, exception) -> log.error("&&&&&&&&&&&&&& Discarding failed record: {}", record),
                        new FixedBackOff(3L, 3)));
    }

    @Bean
    public Function<String, String> process(TxCode txCode) {
        return msg -> txCode.run(msg);
    }

    @Component
    class TxCode {

        @Transactional
        String run(String msg) {
            log.info("Received event={}", msg);
            //int val = messageCounter.get();
            //if (val == 3)
            //    throw new RuntimeException("simulate error");
            try {
                Thread.sleep(5_000);
            } catch (InterruptedException ex) {                
            }
            return msg.toUpperCase();
        }
    }

    @Bean
    public Supplier<String> supplier() {
        return () -> {
            //log.info("Sending new message");
            int val = messageCounter.incrementAndGet();
            //if (val == 3)
            //    throw new RuntimeException("simulate error");
            return "Hello World! #" + val;            
        };
    }

    @ServiceActivator(inputChannel = "outboundtopic.errors")
    public void errorProcessHandler(ErrorMessage em) {
        log.error("@@@ !!!!! errorProcessHandler {}", em.toString());
        //throw new RequeueCurrentMessageException("!!!!!!!!!!!!!!!!!! RETRY UNKNOWN PRODUCER ID !!!!!!!!!!!");
    }

    @ServiceActivator(inputChannel = "inboundtopic.errors")
    public void errorsSupplierHandler(ErrorMessage em) {
        log.error("@@@ !!!!! errorSupplierHandler {}", em.toString());
    }

    public static class DefaultProducerInterceptor implements ProducerInterceptor<byte[], byte[]> {

        @Override
        public void configure(Map<String, ?> configs) {
        }

        @Override
        public ProducerRecord<byte[], byte[]> onSend(ProducerRecord<byte[], byte[]> record) {
            return record;
        }

        @Override
        public void onAcknowledgement(RecordMetadata metadata, Exception exception) {            
        }

        @Override
        public void close() {
        }

    }

}

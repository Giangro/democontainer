/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.poste.democontainer.config;

import it.poste.democontainer.service.ConsumerForProducerTransactionService;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.springframework.cloud.stream.binder.kafka.KafkaBindingRebalanceListener;
import org.springframework.cloud.stream.config.ListenerContainerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.kafka.listener.DefaultAfterRollbackProcessor;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.util.backoff.FixedBackOff;

/**
 *
 * @author GIANGR40
 */
@Configuration
@ComponentScan(basePackages = "it.poste")
@EnableTransactionManagement
@EnableScheduling
@EnableRetry
@Slf4j
public class Config {   
        
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
                        (record, exception) -> log.error("Discarding failed record: {}", record),
                        new FixedBackOff(3_000L, 3)));
    }     

    @ServiceActivator(inputChannel = "outboundtopic.errors")
    public void errorProcessHandler(ErrorMessage em) {
        log.error("@@@@@@@@@@@@@@@@ errorProcessHandler {}", em.toString());        
    }

    @ServiceActivator(inputChannel = "inboundtopic.errors")
    public void errorsSupplierHandler(ErrorMessage em) {
        log.error("@@@@@@@@@@@@@@@@ errorSupplierHandler {}", em.toString());
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

    @PostConstruct
    public void init() {      
    }
    
}

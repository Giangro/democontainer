/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.poste.democontainer.config;

import java.util.Collection;
import java.util.Map;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.springframework.cloud.stream.binder.BinderFactory;
import org.springframework.cloud.stream.binder.kafka.KafkaBindingRebalanceListener;
import org.springframework.cloud.stream.binder.kafka.KafkaMessageChannelBinder;
import org.springframework.cloud.stream.config.ListenerContainerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultAfterRollbackProcessor;
import org.springframework.messaging.MessageChannel;
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

    // https://github.com/spring-cloud/spring-cloud-stream-binder-kafka/issues/946
    
    public KafkaOperations<byte[], byte[]> recoverTemplate(BinderFactory binders) {
        ProducerFactory<byte[], byte[]> pf = ((KafkaMessageChannelBinder) binders.getBinder(null,
                MessageChannel.class)).getTransactionalProducerFactory();
        return new KafkaTemplate<>(pf);
    }

    @Bean
    public ListenerContainerCustomizer<AbstractMessageListenerContainer<?, ?>> customizer(BinderFactory binders,
            GenericApplicationContext ctx) {

        return (container, dest, group) -> {
            ctx.registerBean("recoverTemplate", KafkaOperations.class, () -> recoverTemplate(binders));
            @SuppressWarnings("unchecked")
            KafkaOperations<byte[], byte[]> recoverTemplate = ctx.getBean("recoverTemplate", KafkaOperations.class);
            DefaultAfterRollbackProcessor defaultAfterRollbackProcessor =
                    new DefaultAfterRollbackProcessor<>(
                    new DeadLetterPublishingRecoverer(recoverTemplate,
                            (cr, e) -> new TopicPartition("inboundtopic.DLT", -1)),
                    new FixedBackOff(3000L, 3L), recoverTemplate, true);
            
            // add here not retryable exceptions
            defaultAfterRollbackProcessor.addNotRetryableExceptions(IllegalStateException.class);                        
            container.setAfterRollbackProcessor(defaultAfterRollbackProcessor);
        };
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

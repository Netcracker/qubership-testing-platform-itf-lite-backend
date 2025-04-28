/*
 * # Copyright 2024-2025 NetCracker Technology Corporation
 * #
 * # Licensed under the Apache License, Version 2.0 (the "License");
 * # you may not use this file except in compliance with the License.
 * # You may obtain a copy of the License at
 * #
 * #      http://www.apache.org/licenses/LICENSE-2.0
 * #
 * # Unless required by applicable law or agreed to in writing, software
 * # distributed under the License is distributed on an "AS IS" BASIS,
 * # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * # See the License for the specific language governing permissions and
 * # limitations under the License.
 */

package org.qubership.atp.itf.lite.backend.configuration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.CreateTopicsOptions;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.UUIDDeserializer;
import org.apache.kafka.common.serialization.UUIDSerializer;
import org.qubership.atp.itf.lite.backend.exceptions.internal.ItfLiteKafkaListenerContainerFactoryException;
import org.qubership.atp.itf.lite.backend.model.api.kafka.GetAccessTokenFinish;
import org.qubership.atp.itf.lite.backend.model.api.kafka.ItfExportRequestEvent;
import org.qubership.atp.itf.lite.backend.model.api.kafka.ItfExportResponseEvent;
import org.qubership.atp.itf.lite.backend.model.api.kafka.ItfLiteExecutionFinishEvent;
import org.qubership.atp.itf.lite.backend.model.api.kafka.MiaExportRequestEvent;
import org.qubership.atp.itf.lite.backend.model.api.kafka.MiaExportResponseEvent;
import org.qubership.atp.itf.lite.backend.model.api.kafka.ProjectEvent;
import org.qubership.atp.itf.lite.backend.service.RequestExportService;
import org.qubership.atp.itf.lite.backend.service.SseEmitterService;
import org.qubership.atp.itf.lite.backend.service.kafka.KafkaExecutionFinishResponseService;
import org.qubership.atp.itf.lite.backend.service.kafka.KafkaExecutionFinishSendingService;
import org.qubership.atp.itf.lite.backend.service.kafka.KafkaExportEventExceptionResponseSendingService;
import org.qubership.atp.itf.lite.backend.service.kafka.KafkaExportEventResponseService;
import org.qubership.atp.itf.lite.backend.service.kafka.KafkaExportEventSendingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableKafka
@Slf4j
public class KafkaConfiguration {

    public static final String CATALOG_PROJECT_EVENT_CONTAINER_FACTORY = "catalogProjectEventContainerFactory";
    @Value("${kafka.itflite.export.mia.start.topic}")
    public String kafkaItfLiteExportMiaStartTopic;
    @Value("${kafka.itflite.export.mia.finish.topic}")
    public String kafkaItfLiteExportMiaFinishTopic;
    @Value("${kafka.itflite.export.mia.replicas:3}")
    public int kafkaItfLiteExportMiaReplicas;
    @Value("${kafka.itflite.export.mia.partitions:3}")
    public int kafkaItfLiteExportMiaPartitions;
    @Value("${kafka.itflite.export.itf.start.topic}")
    public String kafkaItfLiteExportItfStartTopic;
    @Value("${kafka.itflite.export.itf.finish.topic}")
    public String kafkaItfLiteExportItfFinishTopic;
    @Value("${kafka.itflite.export.itf.replicas:3}")
    public int kafkaItfLiteExportItfReplicas;
    @Value("${kafka.itflite.export.itf.partitions:3}")
    public int kafkaItfLiteExportItfPartitions;
    @Value("${kafka.itflite.execution.finish.topic}")
    public String kafkaItfLiteExecutionFinishTopic;
    @Value("${kafka.itflite.execution.finish.replicas:3}")
    public int kafkaItfLiteExecutionFinishReplicas;
    @Value("${kafka.itflite.execution.finish.partitions:3}")
    public int kafkaItfLiteExecutionFinishPartitions;
    @Value("${kafka.itflite.getaccesstoken.finish.topic}")
    public String kafkaItfLiteGetAccessTokenFinishTopic;
    @Value("${kafka.itflite.getaccesstoken.finish.replicas:3}")
    public int kafkaItfLiteGetAccessTokenFinishReplicas;
    @Value("${kafka.itflite.getaccesstoken.finish.partitions:3}")
    public int kafkaItfLiteGetAccessTokenFinishPartitions;
    @Value("${spring.kafka.bootstrap-servers}")
    public String bootstrapServers;

    public static final String MIA_EXPORT_KAFKA_TEMPLATE_BEAN_NAME = "miaExportKafkaTemplate";
    public static final String MIA_FINISH_EXPORT_KAFKA_TEMPLATE_BEAN_NAME = "miaFinishExportKafkaTemplate";
    public static final String MIA_EXPORT_KAFKA_CONTAINER_FACTORY_BEAN_NAME = "miaExportContainerFactory";
    public static final String GET_ACCESS_TOKEN_KAFKA_TEMPLATE_BEAN_NAME = "getAccessTokenKafkaTemplate";
    public static final String GET_ACCESS_TOKEN_KAFKA_CONTAINER_FACTORY_BEAN_NAME = "getAccessTokenContainerFactory";
    public static final String ITF_EXPORT_KAFKA_TEMPLATE_BEAN_NAME = "itfExportKafkaTemplate";
    public static final String ITF_FINISH_EXPORT_KAFKA_TEMPLATE_BEAN_NAME = "itfFinishExportKafkaTemplate";
    public static final String ITF_EXPORT_KAFKA_CONTAINER_FACTORY_BEAN_NAME = "itfExportContainerFactory";
    public static final String ITF_LITE_EXECUTION_FINISH_TEMPLATE_BEAN_NAME = "itfLiteExecutionFinishKafkaTemplate";
    public static final String ITF_LITE_EXECUTION_FINISH_CONTAINER_FACTORY_BEAN_NAME =
            "itfLiteExecutionFinishContainerFactory";
    public static final String ENVIRONMENT_KAFKA_CONTAINER_FACTORY_BEAN_NAME = "environmentContainerFactory";

    /**
     * Configure kafka admin.
     *
     * @return configured kafka admin.
     */
    @Bean
    public KafkaAdmin admin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configs.put(AdminClientConfig.RETRIES_CONFIG, 3);
        return new KafkaAdmin(configs);
    }

    /**
     * Container factory for kafka listener.
     * @return concurrent kafka listener container factory
     */
    @Bean(ITF_LITE_EXECUTION_FINISH_CONTAINER_FACTORY_BEAN_NAME)
    public ConcurrentKafkaListenerContainerFactory<UUID, String> itfLiteExecutionFinishKafkaListenerContainerFactory() {
        log.debug("Start itf-lite execution finish kafka container factory: {}",
                ITF_LITE_EXECUTION_FINISH_CONTAINER_FACTORY_BEAN_NAME);
        ConcurrentKafkaListenerContainerFactory<UUID, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(itfLiteExecutionFinishConsumerFactory());
        factory.setMessageConverter(new StringJsonMessageConverter());
        return factory;
    }

    /**
     * Consumer factory for request execution finish kafka consumer factory.
     * Adds consumer group
     * @return concurrent kafka listener container factory
     */
    public ConsumerFactory<UUID, String> itfLiteExecutionFinishConsumerFactory() {
        log.debug("itf-lite execution finish consumer factory configuration.");
        Map<String, Object> props = consumerFactoryProperties(ItfLiteExecutionFinishEvent.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Creates new producer factory and kafka template based on producer factory.
     * @return new kafka template
     */
    @Bean(name = ITF_LITE_EXECUTION_FINISH_TEMPLATE_BEAN_NAME)
    public KafkaTemplate<UUID, ItfLiteExecutionFinishEvent> itfLiteExecutionFinishKafkaTemplate() {
        log.debug("Create itf-lite execution finish kafkaTemplate bean.");
        createOrUpdateTopic(kafkaItfLiteExecutionFinishTopic, kafkaItfLiteExecutionFinishPartitions,
                kafkaItfLiteExecutionFinishReplicas);
        Map<String, Object> configProps = producerConfigProperties();
        ProducerFactory<UUID, ItfLiteExecutionFinishEvent> producerFactory =
                new DefaultKafkaProducerFactory<>(configProps);
        return new KafkaTemplate<>(producerFactory);
    }

    /**
     * Creates kafka execution finish sending service.
     *
     * @return KafkaExecutionFinishSendingService.
     */
    @Bean
    public KafkaExecutionFinishSendingService kafkaExecutionFinishSendingService(
            KafkaTemplate<UUID, ItfLiteExecutionFinishEvent> itfLiteExecutionFinishKafkaTemplate) {
            return new KafkaExecutionFinishSendingService(
                    kafkaItfLiteExecutionFinishTopic, itfLiteExecutionFinishKafkaTemplate);
    }

    /**
     * Creates KafkaExecutionFinishResponseService.
     *
     * @return - KafkaExecutionFinishResponseService.
     */
    @Bean
    public KafkaExecutionFinishResponseService kafkaExecutionFinishResponseService(
            SseEmitterService sseEmitterService) {
        return new KafkaExecutionFinishResponseService(sseEmitterService);
    }

    /**
     * Creates new producer factory and kafka template based on producer factory.
     *
     * @return new kafka template
     */
    @Bean(name = MIA_EXPORT_KAFKA_TEMPLATE_BEAN_NAME)
    public KafkaTemplate<UUID, MiaExportRequestEvent> miaExportKafkaTemplate() {
        createOrUpdateTopic(kafkaItfLiteExportMiaStartTopic, kafkaItfLiteExportMiaPartitions,
                kafkaItfLiteExportMiaReplicas);
        Map<String, Object> configProps = producerConfigProperties();
        ProducerFactory<UUID, MiaExportRequestEvent> producerFactory = new DefaultKafkaProducerFactory<>(configProps);
        return new KafkaTemplate<>(producerFactory);
    }

    /**
     * Creates new producer factory and kafka template based on producer factory.
     *
     * @return new kafka template
     */
    @Bean(name = ITF_EXPORT_KAFKA_TEMPLATE_BEAN_NAME)
    public KafkaTemplate<UUID, ItfExportRequestEvent> itfExportKafkaTemplate() {
        createOrUpdateTopic(kafkaItfLiteExportItfStartTopic, kafkaItfLiteExportItfPartitions,
                kafkaItfLiteExportItfReplicas);
        Map<String, Object> configProps = producerConfigProperties();
        ProducerFactory<UUID, ItfExportRequestEvent> producerFactory = new DefaultKafkaProducerFactory<>(configProps);
        return new KafkaTemplate<>(producerFactory);
    }

    /**
     * Creates new producer factory and kafka template based on producer factory.
     * @return new kafka template
     */
    @Bean(name = MIA_FINISH_EXPORT_KAFKA_TEMPLATE_BEAN_NAME)
    public KafkaTemplate<UUID, MiaExportResponseEvent> miaFinishExportKafkaTemplate() {
        createOrUpdateTopic(kafkaItfLiteExportMiaFinishTopic, kafkaItfLiteExportMiaPartitions,
                kafkaItfLiteExportMiaReplicas);
        Map<String, Object> configProps = producerConfigProperties();
        ProducerFactory<UUID, MiaExportResponseEvent> producerFactory = new DefaultKafkaProducerFactory<>(configProps);
        return new KafkaTemplate<>(producerFactory);
    }

    /**
     * Creates new producer factory and kafka template based on producer factory.
     * @return new kafka template
     */
    @Bean(name = ITF_FINISH_EXPORT_KAFKA_TEMPLATE_BEAN_NAME)
    public KafkaTemplate<UUID, ItfExportResponseEvent> itfFinishExportKafkaTemplate() {
        createOrUpdateTopic(kafkaItfLiteExportItfFinishTopic, kafkaItfLiteExportItfPartitions,
                kafkaItfLiteExportItfReplicas);
        Map<String, Object> configProps = producerConfigProperties();
        ProducerFactory<UUID, ItfExportResponseEvent> producerFactory = new DefaultKafkaProducerFactory<>(configProps);
        return new KafkaTemplate<>(producerFactory);
    }

    private Map<String, Object> producerConfigProperties() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, UUIDSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return configProps;
    }

    /**
     * Creates KafkaExportEventSendingService.
     *
     * @return - KafkaExportEventSendingService.
     */
    @Bean
    public KafkaExportEventSendingService kafkaExportEventSendingService(
            KafkaTemplate<UUID, MiaExportRequestEvent> miaExportKafkaTemplate,
            KafkaTemplate<UUID, ItfExportRequestEvent> itfExportKafkaTemplate) {
        return new KafkaExportEventSendingService(kafkaItfLiteExportMiaStartTopic, miaExportKafkaTemplate,
                kafkaItfLiteExportItfStartTopic, itfExportKafkaTemplate);
    }

    /**
     * Creates KafkaExportEventSendingService.
     *
     * @return - KafkaExportEventSendingService.
     */
    @Bean
    public KafkaExportEventExceptionResponseSendingService kafkaExportEventExceptionResponseSendingService(
            KafkaTemplate<UUID, MiaExportResponseEvent> miaFinishExportKafkaTemplate,
            KafkaTemplate<UUID, ItfExportResponseEvent> itfFinishExportKafkaTemplate) {
        return new KafkaExportEventExceptionResponseSendingService(
                kafkaItfLiteExportMiaFinishTopic, miaFinishExportKafkaTemplate,
                kafkaItfLiteExportItfFinishTopic, itfFinishExportKafkaTemplate);
    }

    /**
     * Creates KafkaExportEventResponseService.
     *
     * @return - KafkaExportEventSendingService.
     */
    @Bean
    public KafkaExportEventResponseService kafkaExportEventResponseService(SseEmitterService sseEmitterService,
                                                                           RequestExportService requestExportService) {
        return new KafkaExportEventResponseService(sseEmitterService, requestExportService);
    }

    /**
     * Factory access token for kafka listener.
     */
    @Bean(GET_ACCESS_TOKEN_KAFKA_CONTAINER_FACTORY_BEAN_NAME)
    public ConcurrentKafkaListenerContainerFactory<UUID, GetAccessTokenFinish>
    getAccessTokenKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<UUID, GetAccessTokenFinish> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(getAccessTokenConsumerFactory());
        factory.setMessageConverter(new StringJsonMessageConverter());
        factory.setErrorHandler((e, consumerRecord) -> {
            log.error("Error during kafka event processing in {}, consumerRecord: {}",
                    GET_ACCESS_TOKEN_KAFKA_CONTAINER_FACTORY_BEAN_NAME, consumerRecord, e);
            throw new ItfLiteKafkaListenerContainerFactoryException();
        });
        return factory;
    }

    /**
     * Consumer factory for kafka listener.
     *
     * @return concurrent kafka listener container factory
     */
    public ConsumerFactory<UUID, GetAccessTokenFinish> getAccessTokenConsumerFactory() {
        Map<String, Object> props = consumerFactoryProperties(GetAccessTokenFinish.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Creates new producer factory and kafka template based on producer factory.
     *
     * @return new kafka template
     */
    @Bean(name = GET_ACCESS_TOKEN_KAFKA_TEMPLATE_BEAN_NAME)
    public KafkaTemplate<UUID, GetAccessTokenFinish> getAccessTokenFinishKafkaTemplate() {
        createOrUpdateTopic(kafkaItfLiteGetAccessTokenFinishTopic, kafkaItfLiteGetAccessTokenFinishPartitions,
                kafkaItfLiteGetAccessTokenFinishReplicas);
        Map<String, Object> configProps = producerConfigProperties();
        ProducerFactory<UUID, GetAccessTokenFinish> producerFactory = new DefaultKafkaProducerFactory<>(configProps);
        return new KafkaTemplate<>(producerFactory);
    }

    /**
     * Creates or updates topic.
     *
     * @param kafkaNotificationTopic      topic
     * @param kafkaNotificationPartitions partitions
     * @param kafkaNotificationReplicas   replicas
     */
    private void createOrUpdateTopic(String kafkaNotificationTopic, Integer kafkaNotificationPartitions,
                                     Integer kafkaNotificationReplicas) {
        log.info("Start createOrUpdateTopic: create or update topic [name={}, partitions={}, replicationFactor={}]",
                kafkaNotificationTopic, kafkaNotificationPartitions, kafkaNotificationReplicas);
        try (AdminClient client = AdminClient.create(admin().getConfigurationProperties())) {
            ListTopicsResult ltr = client.listTopics();
            Set<String> existingTopics = ltr.names().get();
            if (existingTopics.contains(kafkaNotificationTopic)) {
                log.debug("createOrUpdateTopic: update topic [name={}, partitions={}]", kafkaNotificationTopic,
                        kafkaNotificationPartitions);
                Map<String, NewPartitions> newPartitionSet = new HashMap<>();
                newPartitionSet.put(kafkaNotificationTopic,
                        NewPartitions.increaseTo(kafkaNotificationPartitions));
                client.createPartitions(newPartitionSet).all().get();
            } else {
                log.debug("createOrUpdateTopic: create new topic [name={}, partitions={}]", kafkaNotificationTopic,
                        kafkaNotificationPartitions);
                client.createTopics(Collections.singleton(
                                topic(kafkaNotificationTopic, kafkaNotificationPartitions, kafkaNotificationReplicas)),
                        new CreateTopicsOptions().timeoutMs(10000)).all().get();
            }
        } catch (Exception ex) {
            log.error("Cannot create topic [name={}, partitions={}, replicas={}]", kafkaNotificationTopic,
                    kafkaNotificationPartitions, kafkaNotificationReplicas);
        }
    }

    /**
     * Configure kafkaNotificationTopic via kafkaNotificationPartitions and kafkaNotificationReplicas.
     *
     * @param kafkaNotificationTopic      topic
     * @param kafkaNotificationPartitions partitions
     * @param kafkaNotificationReplicas   replicas
     * @return new topic.
     */
    private NewTopic topic(String kafkaNotificationTopic, Integer kafkaNotificationPartitions,
                           Integer kafkaNotificationReplicas) {
        return TopicBuilder.name(kafkaNotificationTopic)
                .partitions(kafkaNotificationPartitions)
                .replicas(kafkaNotificationReplicas)
                .build();
    }

    /**
     * Properties for consumer factory.
     *
     * @param exportResponseEventClazz export response event clazz (e.g. MiaExportResponseEvent or
     *                                 ItfExportResponseEvent)
     * @return map with properties
     */
    private Map<String, Object> consumerFactoryProperties(Class<?> exportResponseEventClazz) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, UUIDDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, StringDeserializer.class);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, exportResponseEventClazz);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);// we are manually committing the message
        return props;
    }

    /**
     * Consumer factory for kafka listener.
     *
     * @return concurrent kafka listener container factory
     */
    public ConsumerFactory<UUID, String> miaConsumerFactory() {
        Map<String, Object> props = consumerFactoryProperties(MiaExportResponseEvent.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Container factory for kafka listener.
     *
     * @return concurrent kafka listener container factory
     */
    @Bean(CATALOG_PROJECT_EVENT_CONTAINER_FACTORY)
    public ConcurrentKafkaListenerContainerFactory<UUID, String> catalogProjectEventContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<UUID, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(catalogConsumerFactory());
        factory.setMessageConverter(new StringJsonMessageConverter());
        factory.setErrorHandler((e, consumerRecord) -> {
            log.error("Error during kafka event processing in {}, consumerRecord: {}",
                    CATALOG_PROJECT_EVENT_CONTAINER_FACTORY, consumerRecord, e);
            throw new ItfLiteKafkaListenerContainerFactoryException();
        });
        return factory;
    }

    /**
     * Consumer factory for kafka listener.
     *
     * @return concurrent kafka listener container factory
     */
    public ConsumerFactory<UUID, String> catalogConsumerFactory() {
        Map<String, Object> props = consumerFactoryProperties(ProjectEvent.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Container factory for kafka listener.
     *
     * @return concurrent kafka listener container factory
     */
    @Bean(MIA_EXPORT_KAFKA_CONTAINER_FACTORY_BEAN_NAME)
    public ConcurrentKafkaListenerContainerFactory<UUID, String> miaKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<UUID, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(miaConsumerFactory());
        factory.setMessageConverter(new StringJsonMessageConverter());
        factory.setErrorHandler((e, consumerRecord) -> {
            log.error("Error during kafka event processing in {}, consumerRecord: {}",
                    MIA_EXPORT_KAFKA_CONTAINER_FACTORY_BEAN_NAME, consumerRecord, e);
            throw new ItfLiteKafkaListenerContainerFactoryException();
        });
        return factory;
    }

    /**
     * Consumer factory for kafka listener.
     *
     * @return concurrent kafka listener container factory
     */
    public ConsumerFactory<UUID, String> itfConsumerFactory() {
        Map<String, Object> props = consumerFactoryProperties(ItfExportResponseEvent.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Container factory for kafka listener.
     *
     * @return concurrent kafka listener container factory
     */
    @Bean(ITF_EXPORT_KAFKA_CONTAINER_FACTORY_BEAN_NAME)
    public ConcurrentKafkaListenerContainerFactory<UUID, String> itfKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<UUID, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(itfConsumerFactory());
        factory.setMessageConverter(new StringJsonMessageConverter());
        factory.setErrorHandler((e, consumerRecord) -> {
            log.error("Error during kafka event processing in {}, consumerRecord: {}",
                    ITF_EXPORT_KAFKA_CONTAINER_FACTORY_BEAN_NAME, consumerRecord, e);
            throw new ItfLiteKafkaListenerContainerFactoryException();
        });
        return factory;
    }

    /**
     * Container factory for kafka listener.
     *
     * @return concurrent kafka listener container factory
     */
    @Bean(ENVIRONMENT_KAFKA_CONTAINER_FACTORY_BEAN_NAME)
    public ConcurrentKafkaListenerContainerFactory<UUID, String> environmentKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<UUID, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(environmentConsumerFactory());
        factory.setMessageConverter(new StringJsonMessageConverter());
        factory.setErrorHandler((e, consumerRecord) -> {
            log.error("Error during kafka event processing in {}, consumerRecord: {}",
                    ENVIRONMENT_KAFKA_CONTAINER_FACTORY_BEAN_NAME, consumerRecord, e);
            throw new ItfLiteKafkaListenerContainerFactoryException();
        });
        return factory;
    }

    /**
     * Consumer factory for kafka listener.
     *
     * @return concurrent kafka listener container factory
     */
    public ConsumerFactory<UUID, String> environmentConsumerFactory() {
        Map<String, Object> props = consumerFactoryProperties(String.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }
}

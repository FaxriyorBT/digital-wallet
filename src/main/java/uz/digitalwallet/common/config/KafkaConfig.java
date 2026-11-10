package uz.digitalwallet.common.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.support.converter.StringJacksonJsonMessageConverter;
import org.springframework.util.backoff.FixedBackOff;
import tools.jackson.databind.json.JsonMapper;
import uz.digitalwallet.event.Topics;

@Configuration(proxyBeanMethods = false)
public class KafkaConfig {

    private static final int PARTITIONS = 3;

    @Bean
    public KafkaAdmin.NewTopics walletTopics() {
        return new KafkaAdmin.NewTopics(
                topic(Topics.USER_REGISTERED),
                topic(Topics.USER_REGISTERED + Topics.DEAD_LETTER_SUFFIX),
                topic(Topics.TRANSFER_COMPLETED),
                topic(Topics.TRANSFER_COMPLETED + Topics.DEAD_LETTER_SUFFIX));
    }

    /**
     * A failing message is retried three times with a one second pause and then moved to "<topic>.DLT",
     * so one broken message cannot block the whole partition. Errors that retrying cannot fix
     * (for example a payload that is not valid JSON) go to the DLT immediately.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (consumerRecord, exception) -> new TopicPartition(
                        consumerRecord.topic() + Topics.DEAD_LETTER_SUFFIX, consumerRecord.partition()));
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1_000L, 3));
    }

    /**
     * Lets @KafkaListener methods receive event records directly instead of raw JSON strings.
     */
    @Bean
    public RecordMessageConverter kafkaMessageConverter(JsonMapper jsonMapper) {
        return new StringJacksonJsonMessageConverter(jsonMapper);
    }

    private static NewTopic topic(String name) {
        return TopicBuilder.name(name).partitions(PARTITIONS).replicas(1).build();
    }
}

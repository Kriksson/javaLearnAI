package learning.task102;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@SpringBootApplication
public class BankApplication {
    public static void main(String[] args) {
        SpringApplication.run(BankApplication.class, args);
    }

    @Bean
    NewTopic bankSmsTopic() {
      return TopicBuilder.name("bank.sms.v1").partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic bankSmsErrorTopic() {
        return TopicBuilder.name("bank.sms.v1.DLT").partitions(3).replicas(1).build();
    }

    @Bean
    DefaultErrorHandler errorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer
                = new DeadLetterPublishingRecoverer(kafkaTemplate, (record, exception) ->
                new TopicPartition("bank.sms.v1.DLT", record.partition()));

        recoverer.setFailIfSendResultIsError(true);

        return new DefaultErrorHandler(recoverer, new FixedBackOff(100L, 2L));
    }
}

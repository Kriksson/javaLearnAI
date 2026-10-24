package learning.task101;

public record KafkaMessageDTO(Long clientId, String text, int partition, long offset) {
}

package learning.task102.DTO;

public record KafkaMessageDTO(Long clientId, String text, int partition, long offset) {
}

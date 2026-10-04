package learning.task067;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "guild.raids")
public record RaidSettings(int maxParticipantsPerRaid) {
}

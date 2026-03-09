package horizon.SeRVe.core.dto.scenario;

import horizon.SeRVe.core.entity.Scenario;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ScenarioResponse {
    private String scenarioId;
    private String promptText;
    private String promptHash;
    private LocalDateTime createdAt;

    public static ScenarioResponse from(Scenario scenario) {
        return ScenarioResponse.builder()
                .scenarioId(scenario.getScenarioId())
                .promptText(scenario.getPromptText())
                .promptHash(scenario.getPromptHash())
                .createdAt(scenario.getCreatedAt())
                .build();
    }
}

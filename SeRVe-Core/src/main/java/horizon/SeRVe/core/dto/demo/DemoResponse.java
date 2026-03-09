package horizon.SeRVe.core.dto.demo;

import horizon.SeRVe.core.entity.Demo;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DemoResponse {
    private String demoId;
    private String scenarioId;
    private String status;
    private Integer numSteps;
    private Integer stateDim;
    private Integer actionDim;
    private Integer imageH;
    private Integer imageW;
    private Integer embedDim;
    private String embedModelId;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;

    public static DemoResponse from(Demo demo) {
        return DemoResponse.builder()
                .demoId(demo.getDemoId())
                .scenarioId(demo.getScenario().getScenarioId())
                .status(demo.getStatus())
                .numSteps(demo.getNumSteps())
                .stateDim(demo.getStateDim())
                .actionDim(demo.getActionDim())
                .imageH(demo.getImageH())
                .imageW(demo.getImageW())
                .embedDim(demo.getEmbedDim())
                .embedModelId(demo.getEmbedModelId())
                .createdAt(demo.getCreatedAt())
                .approvedAt(demo.getApprovedAt())
                .build();
    }
}

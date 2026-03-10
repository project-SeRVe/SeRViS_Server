package horizon.SeRVe.core.dto.demo;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DemoCreateRequest {
    private Integer numSteps;
    private Integer stateDim;
    private Integer actionDim;
    private Integer imageH;
    private Integer imageW;
    private Integer embedDim;
    private String embedModelId;
}

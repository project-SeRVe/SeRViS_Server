package horizon.SeRVe.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RobotLoginRequest {
    @NotBlank(message = "시리얼 번호는 필수입니다.")
    private String serialNumber;

    @NotBlank(message = "API 토큰은 필수입니다.")
    private String apiToken;
}

package horizon.SeRVe.core.dto.task;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ClientUploadRequest {
    private String content;
    private String repositoryId;
}

package horizon.SeRVe.core.dto.sync;

import horizon.SeRVe.core.entity.Task;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangedTaskResponse {
    private String taskId;
    private String fileName;
    private String fileType;
    private int version;
    private String uploaderId;

    public static ChangedTaskResponse from(Task task) {
        return ChangedTaskResponse.builder()
                .taskId(task.getTaskId())
                .fileName(task.getOriginalFileName())
                .fileType(task.getFileType())
                .version(task.getEncryptedData().getVersion())
                .uploaderId(task.getUploaderId())
                .build();
    }
}

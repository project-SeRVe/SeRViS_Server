package horizon.SeRVe.core.dto.task;

import horizon.SeRVe.core.entity.Task;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TaskResponse {
    private Long id;
    private String fileName;
    private String fileType;
    private String uploaderId;
    private LocalDateTime createdAt;

    public static TaskResponse from(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .fileName(task.getOriginalFileName())
                .fileType(task.getFileType())
                .uploaderId(task.getUploaderId())
                .createdAt(task.getUploadedAt())
                .build();
    }
}

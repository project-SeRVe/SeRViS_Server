package horizon.SeRVe.core.controller;

import horizon.SeRVe.core.dto.task.ClientUploadRequest;
import horizon.SeRVe.core.dto.task.TaskResponse;
import horizon.SeRVe.core.dto.task.EncryptedDataResponse;
import horizon.SeRVe.core.dto.task.UploadTaskRequest;
import horizon.SeRVe.core.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    // 기존 업로드 (내부 API용)
    @PostMapping("/api/teams/{teamId}/tasks")
    public ResponseEntity<Void> uploadTask(
            @PathVariable String teamId,
            Authentication authentication,
            @RequestBody UploadTaskRequest request) {

        String userId = (String) authentication.getPrincipal();
        taskService.uploadTask(teamId, userId, request);
        return ResponseEntity.ok().build();
    }

    // 클라이언트 호환 업로드 (POST /api/tasks)
    @PostMapping("/api/tasks")
    public ResponseEntity<Long> uploadTaskFromClient(
            Authentication authentication,
            @RequestBody ClientUploadRequest request) {

        String userId = (String) authentication.getPrincipal();
        Long taskId = taskService.uploadTaskFromClient(
                request.getRepositoryId(), userId, request.getContent());
        return ResponseEntity.ok(taskId);
    }

    @GetMapping("/api/teams/{teamId}/tasks")
    public ResponseEntity<List<TaskResponse>> getTasks(
            @PathVariable String teamId,
            Authentication authentication) {

        String userId = (String) authentication.getPrincipal();
        List<TaskResponse> response = taskService.getTasks(teamId, userId);
        return ResponseEntity.ok(response);
    }

    // 다운로드 (Long id 기반 - 클라이언트 호환)
    @GetMapping("/api/tasks/{id}/data")
    public ResponseEntity<EncryptedDataResponse> downloadData(
            @PathVariable Long id,
            Authentication authentication) {

        String userId = (String) authentication.getPrincipal();
        EncryptedDataResponse response = taskService.getDataById(id, userId);
        return ResponseEntity.ok(response);
    }

    // Presigned URL 발급 (클라이언트가 S3에서 직접 다운로드)
    @GetMapping("/api/tasks/{id}/presigned-url")
    public ResponseEntity<Map<String, String>> getPresignedUrl(
            @PathVariable Long id,
            Authentication authentication) {

        String userId = (String) authentication.getPrincipal();
        String url = taskService.getPresignedUrl(id, userId);
        return ResponseEntity.ok(Map.of("presignedUrl", url));
    }

    @DeleteMapping("/api/teams/{teamId}/tasks/{taskId}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable String teamId,
            @PathVariable String taskId,
            Authentication authentication) {

        String userId = (String) authentication.getPrincipal();
        taskService.deleteTask(taskId, userId);
        return ResponseEntity.ok().build();
    }
}

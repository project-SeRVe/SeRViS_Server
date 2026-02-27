package horizon.SeRVe.core.service;

import horizon.SeRVe.common.dto.feign.MemberRoleResponse;
import horizon.SeRVe.core.dto.task.*;
import horizon.SeRVe.core.entity.*;
import horizon.SeRVe.core.feign.AuthServiceClient;
import horizon.SeRVe.core.feign.TeamServiceClient;
import horizon.SeRVe.core.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final EncryptedDataRepository encryptedDataRepository;
    private final VectorDemoRepository vectorDemoRepository;
    private final TeamServiceClient teamServiceClient;
    private final AuthServiceClient authServiceClient;

    @Transactional
    public void uploadTask(String teamId, String userId, UploadTaskRequest req) {
        // 1. 팀 존재 확인
        if (!teamServiceClient.teamExists(teamId)) {
            throw new IllegalArgumentException("저장소를 찾을 수 없습니다.");
        }

        // 2. 멤버십 및 권한 검증
        MemberRoleResponse memberRole = teamServiceClient.getMemberRole(teamId, userId);

        if (!"ADMIN".equals(memberRole.getRole())) {
            throw new SecurityException("태스크 업로드는 ADMIN 권한이 필요합니다.");
        }

        // 3. 암호화 데이터(Blob) 변환 및 생성
        byte[] blobData = Base64.getDecoder().decode(req.getEncryptedBlob());

        // 같은 이름의 파일이 있는지 확인
        Optional<Task> existingTask = taskRepository.findByTeamIdAndOriginalFileName(teamId, req.getFileName());

        if (existingTask.isPresent()) {
            // [Case A] 이미 존재함 -> 업데이트 (Version Up)
            Task task = existingTask.get();
            EncryptedData data = task.getEncryptedData();

            data.updateContent(blobData);

        } else {
            // [Case B] 없음 -> 신규 생성 (Version 1)
            Task task = Task.builder()
                    .taskId(UUID.randomUUID().toString())
                    .teamId(teamId)
                    .uploaderId(userId)
                    .originalFileName(req.getFileName())
                    .fileType(req.getFileType())
                    .build();

            EncryptedData encryptedData = EncryptedData.builder()
                    .dataId(UUID.randomUUID().toString())
                    .task(task)
                    .encryptedBlob(blobData)
                    .build();

            task.setEncryptedData(encryptedData);
            taskRepository.save(task);
        }
    }

    // 태스크 목록 조회
    @Transactional(readOnly = true)
    public List<TaskResponse> getTasks(String teamId, String userId) {
        // 팀 존재 확인
        if (!teamServiceClient.teamExists(teamId)) {
            throw new IllegalArgumentException("저장소를 찾을 수 없습니다.");
        }

        // 멤버십 검증 (ADMIN과 MEMBER 모두 조회 가능)
        if (!teamServiceClient.memberExists(teamId, userId)) {
            throw new SecurityException("저장소 멤버가 아닙니다.");
        }

        return taskRepository.findAllByTeamId(teamId).stream()
                .map(TaskResponse::from)
                .collect(Collectors.toList());
    }

    // 클라이언트 호환 업로드 (POST /api/tasks)
    @Transactional
    public Long uploadTaskFromClient(String repositoryId, String userId, String content) {
        // 1. 팀 존재 확인
        if (!teamServiceClient.teamExists(repositoryId)) {
            throw new IllegalArgumentException("저장소를 찾을 수 없습니다.");
        }

        // 2. 멤버십 검증
        teamServiceClient.getMemberRole(repositoryId, userId);

        // 3. 암호화된 콘텐츠를 바이너리로 변환
        byte[] blobData = Base64.getDecoder().decode(content);

        // 4. 태스크 생성
        Task task = Task.builder()
                .taskId(UUID.randomUUID().toString())
                .teamId(repositoryId)
                .uploaderId(userId)
                .originalFileName("uploaded_task")
                .fileType("encrypted")
                .build();

        EncryptedData encryptedData = EncryptedData.builder()
                .dataId(UUID.randomUUID().toString())
                .task(task)
                .encryptedBlob(blobData)
                .build();

        task.setEncryptedData(encryptedData);
        Task saved = taskRepository.save(task);

        return saved.getId();
    }

    // 데이터 다운로드 (Long id 기반 - 클라이언트 호환)
    @Transactional(readOnly = true)
    public EncryptedDataResponse getDataById(Long id, String requesterId) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("태스크를 찾을 수 없습니다."));

        checkTaskPermission(task, requesterId);

        EncryptedData data = encryptedDataRepository.findByTask(task)
                .orElseThrow(() -> new IllegalArgumentException("데이터가 존재하지 않습니다."));

        return EncryptedDataResponse.from(data);
    }

    // 데이터 다운로드 (UUID 기반 - 내부 API용)
    @Transactional(readOnly = true)
    public EncryptedDataResponse getData(String taskId, String requesterId) {
        Task task = taskRepository.findByTaskId(taskId)
                .orElseThrow(() -> new IllegalArgumentException("태스크를 찾을 수 없습니다."));

        checkTaskPermission(task, requesterId);

        EncryptedData data = encryptedDataRepository.findByTask(task)
                .orElseThrow(() -> new IllegalArgumentException("데이터가 존재하지 않습니다."));

        return EncryptedDataResponse.from(data);
    }

    // 태스크 접근 권한 체크 (User 또는 EdgeNode)
    private void checkTaskPermission(Task task, String requesterId) {
        boolean hasPermission = false;

        // 1. 사람(User)인지 확인
        try {
            if (authServiceClient.userExists(requesterId)) {
                hasPermission = teamServiceClient.memberExists(task.getTeamId(), requesterId);
            }
        } catch (Exception e) {
            // User 조회 실패 시 무시
        }

        // 2. 로봇(EdgeNode)인지 확인
        if (!hasPermission) {
            try {
                String edgeNodeTeamId = teamServiceClient.getEdgeNodeTeamId(requesterId);
                hasPermission = edgeNodeTeamId.equals(task.getTeamId());
            } catch (Exception e) {
                // EdgeNode 조회 실패 시 무시
            }
        }

        if (!hasPermission) {
            throw new SecurityException("접근 권한이 없습니다 (멤버 아님).");
        }
    }

    // 태스크 삭제
    @Transactional
    public void deleteTask(String taskId, String userId) {
        Task task = taskRepository.findByTaskId(taskId)
                .orElseThrow(() -> new IllegalArgumentException("태스크를 찾을 수 없습니다."));

        boolean isUploader = task.getUploaderId().equals(userId);

        // 멤버십 및 권한 확인
        MemberRoleResponse memberRole = teamServiceClient.getMemberRole(task.getTeamId(), userId);

        boolean isAdmin = "ADMIN".equals(memberRole.getRole());

        if (!isUploader && !isAdmin) {
            throw new SecurityException("삭제 권한이 없습니다.");
        }

        // 연관된 데모도 논리적 삭제 처리
        List<VectorDemo> demos = vectorDemoRepository.findByTaskId(taskId);
        demos.forEach(demo -> demo.markAsDeleted());

        taskRepository.delete(task);
    }
}

package horizon.SeRVe.core.service;

import horizon.SeRVe.common.dto.feign.MemberRoleResponse;
import horizon.SeRVe.common.dto.feign.UserInfoResponse;
import horizon.SeRVe.common.service.RateLimitService;
import horizon.SeRVe.core.dto.demo.*;
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
public class DemoService {

    private final VectorDemoRepository vectorDemoRepository;
    private final TaskRepository taskRepository;
    private final TeamServiceClient teamServiceClient;
    private final AuthServiceClient authServiceClient;
    private final RateLimitService rateLimitService;

    @Transactional
    public void uploadDemos(String teamId, String fileName, String userId, DemoUploadRequest request) {
        // 1. 팀 존재 확인
        if (!teamServiceClient.teamExists(teamId)) {
            throw new IllegalArgumentException("팀을 찾을 수 없습니다.");
        }

        // 1-1. Rate Limit 체크
        rateLimitService.checkAndRecordUpload(userId);

        // 2. 멤버십 및 권한 체크 (Federated Model: MEMBER 전용, ADMIN은 Key Master 역할만)
        MemberRoleResponse memberRole = teamServiceClient.getMemberRole(teamId, userId);

        // ADMIN은 업로드 금지 (Key Master 역할만 수행)
        if ("ADMIN".equals(memberRole.getRole())) {
            throw new SecurityException("ADMIN은 데이터 업로드가 불가능합니다. MEMBER만 업로드할 수 있습니다.");
        }

        // 3. Task 찾거나 생성
        Optional<Task> existingTask = taskRepository.findByTeamIdAndOriginalFileName(teamId, fileName);
        Task task;

        if (existingTask.isPresent()) {
            task = existingTask.get();
            // 3-1. 기존 태스크가 있으면 uploader 검증 (타인의 태스크 수정 방지)
            if (!task.getUploaderId().equals(userId)) {
                throw new SecurityException("타인의 태스크를 수정할 수 없습니다.");
            }
        } else {
            // 3-2. 새 태스크 생성
            task = Task.builder()
                    .taskId(UUID.randomUUID().toString())
                    .teamId(teamId)
                    .uploaderId(userId)
                    .originalFileName(fileName)
                    .fileType("application/octet-stream")
                    .build();
            task = taskRepository.save(task);
        }

        // 4. 각 데모 처리 (UPDATE or INSERT)
        for (DemoUploadItem item : request.getDemos()) {
            byte[] blobData = Base64.getDecoder().decode(item.getEncryptedBlob());

            Optional<VectorDemo> existingDemo = vectorDemoRepository
                    .findByTaskIdAndDemoIndex(task.getTaskId(), item.getDemoIndex());

            if (existingDemo.isPresent()) {
                // UPDATE: 기존 데모 내용 갱신 (version 자동 증가)
                VectorDemo demo = existingDemo.get();
                demo.updateContent(blobData);
                demo.setDeleted(false);
            } else {
                // INSERT: 새 데모 생성 (version = 0)
                VectorDemo newDemo = VectorDemo.builder()
                        .demoId(UUID.randomUUID().toString())
                        .taskId(task.getTaskId())
                        .teamId(teamId)
                        .demoIndex(item.getDemoIndex())
                        .encryptedBlob(blobData)
                        .isDeleted(false)
                        .build();
                vectorDemoRepository.save(newDemo);
            }
        }
    }

    @Transactional
    public void deleteDemo(String teamId, String fileName, int demoIndex, String userId) {
        // 1. 팀 존재 확인
        if (!teamServiceClient.teamExists(teamId)) {
            throw new IllegalArgumentException("팀을 찾을 수 없습니다.");
        }

        // 2. ADMIN 권한 체크
        MemberRoleResponse memberRole = teamServiceClient.getMemberRole(teamId, userId);

        if (!"ADMIN".equals(memberRole.getRole())) {
            throw new SecurityException("데모 삭제는 ADMIN 권한이 필요합니다.");
        }

        // 3. Task 조회
        Task task = taskRepository.findByTeamIdAndOriginalFileName(teamId, fileName)
                .orElseThrow(() -> new IllegalArgumentException("태스크를 찾을 수 없습니다."));

        // 4. 데모 논리적 삭제 (version 자동 증가)
        VectorDemo demo = vectorDemoRepository
                .findByTaskIdAndDemoIndex(task.getTaskId(), demoIndex)
                .orElseThrow(() -> new IllegalArgumentException("데모를 찾을 수 없습니다."));

        demo.markAsDeleted();
    }

    @Transactional(readOnly = true)
    public List<DemoSyncResponse> syncTeamDemos(String teamId, int lastVersion, String userId) {
        // 1. 팀 존재 확인
        if (!teamServiceClient.teamExists(teamId)) {
            throw new IllegalArgumentException("팀을 찾을 수 없습니다.");
        }

        // 2. 팀 멤버십 체크
        if (!teamServiceClient.memberExists(teamId, userId)) {
            throw new SecurityException("팀 멤버가 아닙니다.");
        }

        // 3. 팀의 모든 태스크에서 변경된 데모 조회
        List<VectorDemo> demos = vectorDemoRepository
                .findByTeamIdAndVersionGreaterThanOrderByVersionAsc(teamId, lastVersion);

        // 4. Task 정보 조회 (N+1 방지: IN 쿼리 사용)
        List<String> taskIds = demos.stream()
                .map(VectorDemo::getTaskId)
                .distinct()
                .collect(Collectors.toList());

        // Task ID → Uploader Email 매핑 생성
        java.util.Map<String, String> taskUploaderMap = taskRepository
                .findAllByTaskIdIn(taskIds)
                .stream()
                .collect(Collectors.toMap(
                        Task::getTaskId,
                        task -> {
                            try {
                                UserInfoResponse userInfo = authServiceClient.getUserInfo(task.getUploaderId());
                                return userInfo.getEmail();
                            } catch (Exception e) {
                                return "unknown";
                            }
                        }
                ));

        // 5. DemoSyncResponse 생성 (createdBy 포함)
        return demos.stream()
                .map(demo -> {
                    String createdBy = taskUploaderMap.getOrDefault(demo.getTaskId(), "unknown");
                    return DemoSyncResponse.from(demo, createdBy);
                })
                .collect(Collectors.toList());
    }
}

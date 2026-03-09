package horizon.SeRVe.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import horizon.SeRVe.common.dto.feign.MemberRoleResponse;
import horizon.SeRVe.core.dto.task.ClientUploadRequest;
import horizon.SeRVe.core.dto.task.TaskResponse;
import horizon.SeRVe.core.dto.task.EncryptedDataResponse;
import horizon.SeRVe.core.entity.Task;
import horizon.SeRVe.core.entity.EncryptedData;
import horizon.SeRVe.core.feign.AuthServiceClient;
import horizon.SeRVe.core.feign.TeamServiceClient;
import horizon.SeRVe.core.repository.TaskRepository;
import horizon.SeRVe.core.repository.EncryptedDataRepository;
import horizon.SeRVe.core.repository.VectorDemoRepository;
import horizon.SeRVe.core.service.S3StorageService;
import horizon.SeRVe.core.service.TaskService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * 클라이언트(client-robot) SDK와의 호환성 검증 테스트
 * ⚠️ S3 전환 후 변경사항:
 *   - EncryptedDataResponse.content(byte[]) → objectKey(String)
 *   - 클라이언트는 objectKey를 받아 S3에서 직접 다운로드해야 함
 */
@ExtendWith(MockitoExtension.class)
class ClientCompatibilityTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private TaskService taskService;

    @Mock private TaskRepository taskRepository;
    @Mock private EncryptedDataRepository encryptedDataRepository;
    @Mock private VectorDemoRepository vectorDemoRepository;
    @Mock private TeamServiceClient teamServiceClient;
    @Mock private AuthServiceClient authServiceClient;
    @Mock private S3StorageService s3StorageService;

    // ==================== DTO 직렬화/역직렬화 테스트 ====================

    @Test
    @DisplayName("태스크 업로드 요청 - 클라이언트 형식 {content, repositoryId} 역직렬화")
    void clientUploadRequest_deserializesFromClientFormat() throws Exception {
        // 클라이언트가 보내는 형식
        String clientJson = """
                {"content": "dGVzdC1jb250ZW50", "repositoryId": "team-uuid-123"}
                """;

        ClientUploadRequest request = objectMapper.readValue(clientJson, ClientUploadRequest.class);

        assertEquals("dGVzdC1jb250ZW50", request.getContent());
        assertEquals("team-uuid-123", request.getRepositoryId());
    }

    @Test
    @DisplayName("태스크 다운로드 응답 - objectKey 필드로 직렬화 (S3 전환 후)")
    void encryptedDataResponse_serializesAsObjectKey() throws Exception {
        EncryptedDataResponse response = EncryptedDataResponse.builder()
                .id(42L)
                .objectKey("team-1/task-uuid/task/file.enc")
                .version(1)
                .build();

        String json = objectMapper.writeValueAsString(response);

        // S3 전환 후: 클라이언트는 objectKey를 받아 S3에서 직접 다운로드
        assertTrue(json.contains("\"objectKey\""), "필드명이 'objectKey'여야 함");
        assertFalse(json.contains("\"encryptedBlob\""), "encryptedBlob 필드가 없어야 함");
        assertFalse(json.contains("\"content\""), "content 필드가 없어야 함 (S3 전환)");
        assertTrue(json.contains("\"id\""), "필드명이 'id'여야 함");
    }

    @Test
    @DisplayName("태스크 목록 응답 - id 필드가 Long 타입")
    void taskResponse_hasLongId() throws Exception {
        Task task = Task.builder()
                .taskId("uuid-123")
                .originalFileName("test.pdf")
                .fileType("pdf")
                .uploaderId("user-1")
                .build();

        var idField = Task.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(task, 42L);

        TaskResponse response = TaskResponse.from(task);
        String json = objectMapper.writeValueAsString(response);

        assertTrue(json.contains("\"id\":42"), "id가 Long 타입 숫자여야 함");
        assertFalse(json.contains("\"taskId\""), "taskId 필드가 없어야 함");
    }

    // ==================== 서비스 로직 테스트 ====================

    @Test
    @DisplayName("클라이언트 호환 업로드 - Long id 반환")
    void uploadTaskFromClient_returnsLongId() {
        // given
        String teamId = "team-1";
        String userId = "user-1";
        String content = Base64.getEncoder().encodeToString("encrypted-data".getBytes());

        MemberRoleResponse memberRole = MemberRoleResponse.builder()
                .userId(userId).teamId(teamId).role("MEMBER").build();

        given(teamServiceClient.teamExists(teamId)).willReturn(true);
        given(teamServiceClient.getMemberRole(teamId, userId)).willReturn(memberRole);
        given(s3StorageService.generateObjectKey(anyString(), anyString(), anyString(), anyString()))
                .willReturn("team-1/task-uuid/task/uploaded_task");
        given(s3StorageService.upload(anyString(), any(byte[].class)))
                .willReturn("team-1/task-uuid/task/uploaded_task");

        given(taskRepository.save(any(Task.class))).willAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            var idField = Task.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(task, 1L);
            return task;
        });

        // when
        Long taskId = taskService.uploadTaskFromClient(teamId, userId, content);

        // then
        assertNotNull(taskId);
        assertEquals(1L, taskId);
    }

    @Test
    @DisplayName("Long id 기반 다운로드 - objectKey 반환")
    void getDataById_returnsObjectKey() {
        // given
        Long id = 42L;
        String userId = "user-1";

        Task mockTask = Task.builder()
                .taskId("uuid-123")
                .teamId("team-1")
                .build();

        EncryptedData mockData = EncryptedData.builder()
                .dataId("data-1")
                .task(mockTask)
                .objectKey("team-1/uuid-123/task/file.enc")
                .build();

        given(taskRepository.findById(id)).willReturn(Optional.of(mockTask));
        given(authServiceClient.userExists(userId)).willReturn(true);
        given(teamServiceClient.memberExists("team-1", userId)).willReturn(true);
        given(encryptedDataRepository.findByTask(mockTask)).willReturn(Optional.of(mockData));

        // when
        EncryptedDataResponse response = taskService.getDataById(id, userId);

        // then
        assertNotNull(response);
        assertEquals("team-1/uuid-123/task/file.enc", response.getObjectKey());
    }

    @Test
    @DisplayName("업로드 응답 값이 클라이언트의 digit 파싱과 호환")
    void uploadResponse_compatibleWithClientParsing() {
        Long serverResponse = 42L;
        String asString = String.valueOf(serverResponse);

        String digits = asString.replaceAll("[^0-9]", "");
        assertEquals("42", digits);
        assertEquals(42, Integer.parseInt(digits));
    }
}

package horizon.SeRVe.core.service;

import horizon.SeRVe.common.dto.feign.MemberRoleResponse;
import horizon.SeRVe.core.dto.task.TaskResponse;
import horizon.SeRVe.core.dto.task.EncryptedDataResponse;
import horizon.SeRVe.core.dto.task.UploadTaskRequest;
import horizon.SeRVe.core.entity.*;
import horizon.SeRVe.core.feign.AuthServiceClient;
import horizon.SeRVe.core.feign.TeamServiceClient;
import horizon.SeRVe.core.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @InjectMocks
    private TaskService taskService;

    @Mock private TaskRepository taskRepository;
    @Mock private EncryptedDataRepository encryptedDataRepository;
    @Mock private VectorDemoRepository vectorDemoRepository;
    @Mock private TeamServiceClient teamServiceClient;
    @Mock private AuthServiceClient authServiceClient;

    @Test
    @DisplayName("태스크 업로드 성공 테스트")
    void uploadTask_Success() {
        // given
        String teamId = "team-1";
        String userId = "user-1";
        String sampleBase64 = Base64.getEncoder().encodeToString("test-content".getBytes());
        UploadTaskRequest request = new UploadTaskRequest("test.pdf", "pdf", sampleBase64);

        MemberRoleResponse memberRole = MemberRoleResponse.builder()
                .userId(userId).teamId(teamId).role("ADMIN").build();

        given(teamServiceClient.teamExists(teamId)).willReturn(true);
        given(teamServiceClient.getMemberRole(teamId, userId)).willReturn(memberRole);

        // when
        taskService.uploadTask(teamId, userId, request);

        // then
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    @DisplayName("태스크 목록 조회 성공 테스트")
    void getTasks_Success() {
        // given
        String teamId = "team-1";
        String userId = "user-1";

        Task task1 = Task.builder()
                .taskId("task-1")
                .originalFileName("file1.pdf")
                .fileType("pdf")
                .uploaderId("uploader-1")
                .build();

        given(teamServiceClient.teamExists(teamId)).willReturn(true);
        given(teamServiceClient.memberExists(teamId, userId)).willReturn(true);
        given(taskRepository.findAllByTeamId(teamId)).willReturn(List.of(task1));

        // when
        List<TaskResponse> result = taskService.getTasks(teamId, userId);

        // then
        assertEquals(1, result.size());
        assertEquals("file1.pdf", result.get(0).getFileName());
        assertEquals("uploader-1", result.get(0).getUploaderId());
    }

    @Test
    @DisplayName("데이터 다운로드 성공 - 멤버일 경우")
    void getData_Success() {
        // given
        String taskId = "task-1";
        String userId = "user-1";

        Task mockTask = Task.builder()
                .taskId(taskId)
                .teamId("team-1")
                .build();

        EncryptedData mockData = EncryptedData.builder()
                .dataId("data-1")
                .task(mockTask)
                .encryptedBlob("secret".getBytes())
                .build();

        given(taskRepository.findByTaskId(taskId)).willReturn(Optional.of(mockTask));
        given(authServiceClient.userExists(userId)).willReturn(true);
        given(teamServiceClient.memberExists("team-1", userId)).willReturn(true);
        given(encryptedDataRepository.findByTask(mockTask)).willReturn(Optional.of(mockData));

        // when
        EncryptedDataResponse response = taskService.getData(taskId, userId);

        // then
        assertNotNull(response);
        assertArrayEquals("secret".getBytes(), response.getContent());
    }

    @Test
    @DisplayName("데이터 다운로드 실패 - 멤버가 아닐 경우")
    void getData_Fail_NotMember() {
        // given
        String taskId = "task-1";
        String userId = "intruder";

        Task mockTask = Task.builder()
                .taskId(taskId)
                .teamId("team-1")
                .build();

        given(taskRepository.findByTaskId(taskId)).willReturn(Optional.of(mockTask));
        given(authServiceClient.userExists(userId)).willReturn(true);
        given(teamServiceClient.memberExists("team-1", userId)).willReturn(false);

        // when & then
        assertThrows(SecurityException.class, () -> {
            taskService.getData(taskId, userId);
        });
    }
}

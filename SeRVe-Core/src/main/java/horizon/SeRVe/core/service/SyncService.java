package horizon.SeRVe.core.service;

import horizon.SeRVe.core.dto.sync.ChangedTaskResponse;
import horizon.SeRVe.core.entity.Task;
import horizon.SeRVe.core.feign.TeamServiceClient;
import horizon.SeRVe.core.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SyncService {

    private final TaskRepository taskRepository;
    private final TeamServiceClient teamServiceClient;

    @Transactional(readOnly = true)
    public List<ChangedTaskResponse> getChangedTasks(String teamId, int lastSyncVersion) {
        if (!teamServiceClient.teamExists(teamId)) {
            throw new IllegalArgumentException("존재하지 않는 팀입니다.");
        }

        // 팀의 모든 태스크 조회
        List<Task> allTasks = taskRepository.findAllByTeamId(teamId);

        // 버전 필터링 (version > lastSyncVersion)
        return allTasks.stream()
                .filter(task -> task.getEncryptedData() != null &&
                        task.getEncryptedData().getVersion() > lastSyncVersion)
                .map(ChangedTaskResponse::from)
                .collect(Collectors.toList());
    }
}

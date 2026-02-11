package horizon.SeRVe.core.controller;

import horizon.SeRVe.core.dto.sync.ChangedTaskResponse;
import horizon.SeRVe.core.service.SyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncController {

    private final SyncService syncService;

    @GetMapping("/tasks")
    public ResponseEntity<List<ChangedTaskResponse>> getChangedTasks(
            @RequestParam String teamId,
            @RequestParam(defaultValue = "0") int lastSyncVersion) {

        List<ChangedTaskResponse> changedTasks =
                syncService.getChangedTasks(teamId, lastSyncVersion);

        return ResponseEntity.ok(changedTasks);
    }
}

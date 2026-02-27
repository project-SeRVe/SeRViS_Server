package horizon.SeRVe.core.controller;

import horizon.SeRVe.core.dto.demo.DemoSyncResponse;
import horizon.SeRVe.core.dto.demo.DemoUploadRequest;
import horizon.SeRVe.core.service.DemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class DemoController {

    private final DemoService demoService;

    @PostMapping("/api/teams/{teamId}/demos")
    public ResponseEntity<Void> uploadDemos(
            @PathVariable String teamId,
            Authentication authentication,
            @RequestBody DemoUploadRequest request) {

        String userId = (String) authentication.getPrincipal();
        demoService.uploadDemos(teamId, request.getFileName(), userId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/api/teams/{teamId}/demos/{demoIndex}")
    public ResponseEntity<Void> deleteDemo(
            @PathVariable String teamId,
            @PathVariable int demoIndex,
            @RequestParam String fileName,
            Authentication authentication) {

        String userId = (String) authentication.getPrincipal();
        demoService.deleteDemo(teamId, fileName, demoIndex, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/sync/demos")
    public ResponseEntity<List<DemoSyncResponse>> syncTeamDemos(
            @RequestParam String teamId,
            @RequestParam(defaultValue = "0") int lastVersion,
            Authentication authentication) {

        String userId = (String) authentication.getPrincipal();
        List<DemoSyncResponse> response = demoService.syncTeamDemos(
                teamId, lastVersion, userId);
        return ResponseEntity.ok(response);
    }
}

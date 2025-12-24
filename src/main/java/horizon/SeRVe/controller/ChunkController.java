package horizon.SeRVe.controller;

import horizon.SeRVe.dto.chunk.ChunkResponse;
import horizon.SeRVe.dto.chunk.ChunkSyncResponse;
import horizon.SeRVe.dto.chunk.ChunkUploadRequest;
import horizon.SeRVe.entity.User;
import horizon.SeRVe.service.ChunkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChunkController {

    private final ChunkService chunkService;

    /**
     * A. 청크 업로드 (배치)
     * POST /api/teams/{teamId}/chunks
     * Body: { "fileName": "설비매뉴얼.pdf", "chunks": [...] }
     */
    @PostMapping("/api/teams/{teamId}/chunks")
    public ResponseEntity<Void> uploadChunks(
            @PathVariable String teamId,
            @AuthenticationPrincipal User user,
            @RequestBody ChunkUploadRequest request) {

        chunkService.uploadChunks(teamId, request.getFileName(), user.getUserId(), request);
        return ResponseEntity.ok().build();
    }

    /**
     * B. 청크 다운로드
     * GET /api/teams/{teamId}/chunks?fileName=설비매뉴얼.pdf
     */
    @GetMapping("/api/teams/{teamId}/chunks")
    public ResponseEntity<List<ChunkResponse>> getChunks(
            @PathVariable String teamId,
            @RequestParam String fileName,
            @AuthenticationPrincipal User user) {

        List<ChunkResponse> response = chunkService.getChunks(teamId, fileName, user.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * C. 청크 삭제 (논리적 삭제)
     * DELETE /api/teams/{teamId}/chunks/{chunkIndex}?fileName=설비매뉴얼.pdf
     */
    @DeleteMapping("/api/teams/{teamId}/chunks/{chunkIndex}")
    public ResponseEntity<Void> deleteChunk(
            @PathVariable String teamId,
            @PathVariable int chunkIndex,
            @RequestParam String fileName,
            @AuthenticationPrincipal User user) {

        chunkService.deleteChunk(teamId, fileName, chunkIndex, user.getUserId());
        return ResponseEntity.ok().build();
    }

    /**
     * E. 팀별 증분 동기화
     * GET /api/sync/chunks?teamId={id}&lastVersion={n}
     */
    @GetMapping("/api/sync/chunks")
    public ResponseEntity<List<ChunkSyncResponse>> syncTeamChunks(
            @RequestParam String teamId,
            @RequestParam(defaultValue = "0") int lastVersion,
            @AuthenticationPrincipal User user) {

        List<ChunkSyncResponse> response = chunkService.syncTeamChunks(
                teamId, lastVersion, user.getUserId());
        return ResponseEntity.ok(response);
    }
}

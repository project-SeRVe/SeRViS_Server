package horizon.SeRVe.core.controller;

import horizon.SeRVe.core.dto.document.ClientUploadRequest;
import horizon.SeRVe.core.dto.document.DocumentResponse;
import horizon.SeRVe.core.dto.document.EncryptedDataResponse;
import horizon.SeRVe.core.dto.document.UploadDocumentRequest;
import horizon.SeRVe.core.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    // 기존 업로드 (내부 API용)
    @PostMapping("/api/teams/{teamId}/documents")
    public ResponseEntity<Void> uploadDocument(
            @PathVariable String teamId,
            Authentication authentication,
            @RequestBody UploadDocumentRequest request) {

        String userId = (String) authentication.getPrincipal();
        documentService.uploadDocument(teamId, userId, request);
        return ResponseEntity.ok().build();
    }

    // 클라이언트 호환 업로드 (POST /api/documents)
    @PostMapping("/api/documents")
    public ResponseEntity<Long> uploadDocumentFromClient(
            Authentication authentication,
            @RequestBody ClientUploadRequest request) {

        String userId = (String) authentication.getPrincipal();
        Long docId = documentService.uploadDocumentFromClient(
                request.getRepositoryId(), userId, request.getContent());
        return ResponseEntity.ok(docId);
    }

    @GetMapping("/api/teams/{teamId}/documents")
    public ResponseEntity<List<DocumentResponse>> getDocuments(
            @PathVariable String teamId,
            Authentication authentication) {

        String userId = (String) authentication.getPrincipal();
        List<DocumentResponse> response = documentService.getDocuments(teamId, userId);
        return ResponseEntity.ok(response);
    }

    // 다운로드 (Long id 기반 - 클라이언트 호환)
    @GetMapping("/api/documents/{id}/data")
    public ResponseEntity<EncryptedDataResponse> downloadData(
            @PathVariable Long id,
            Authentication authentication) {

        String userId = (String) authentication.getPrincipal();
        EncryptedDataResponse response = documentService.getDataById(id, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/api/teams/{teamId}/documents/{docId}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable String teamId,
            @PathVariable String docId,
            Authentication authentication) {

        String userId = (String) authentication.getPrincipal();
        documentService.deleteDocument(docId, userId);
        return ResponseEntity.ok().build();
    }
}

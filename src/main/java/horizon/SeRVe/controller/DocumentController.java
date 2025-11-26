package horizon.SeRVe.controller;

import horizon.SeRVe.dto.security.EncryptedPayloadDto;
import horizon.SeRVe.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping
    public ResponseEntity<String> uploadDocument(@RequestBody EncryptedPayloadDto request) {
        // 실제로는 토큰에서 유저 ID를 꺼내야 하지만, 시뮬레이션을 위해 "tester"로 고정
        String uploaderId = "tester";
        String fileName = "unknown.vector"; // 파일명도 DTO에 추가하면 좋음

        Long docId = documentService.uploadDocument(
                request.getRepositoryId(),
                fileName,
                request.getContent(),
                uploaderId
        );

        return ResponseEntity.ok("문서 업로드 성공! ID: " + docId);
    }
    @GetMapping("/{documentId}")
    public ResponseEntity<EncryptedPayloadDto> getDocument(@PathVariable Long documentId) {
        // 1. DB에서 조회
        horizon.SeRVe.entity.Document doc = documentService.getDocument(documentId);

        // 2. DTO에 포장 (암호문 그대로)
        EncryptedPayloadDto response = new EncryptedPayloadDto();
        response.setRepositoryId(doc.getTeamRepository().getId());
        response.setContent(doc.getEncryptedContent()); // 암호화된 내용

        return ResponseEntity.ok(response);
    }
}
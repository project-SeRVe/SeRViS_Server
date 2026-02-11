package horizon.SeRVe.core.service;

import horizon.SeRVe.common.dto.feign.MemberRoleResponse;
import horizon.SeRVe.core.dto.document.*;
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
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final EncryptedDataRepository encryptedDataRepository;
    private final VectorChunkRepository vectorChunkRepository;
    private final TeamServiceClient teamServiceClient;
    private final AuthServiceClient authServiceClient;

    @Transactional
    public void uploadDocument(String teamId, String userId, UploadDocumentRequest req) {
        // 1. 팀 존재 확인
        if (!teamServiceClient.teamExists(teamId)) {
            throw new IllegalArgumentException("저장소를 찾을 수 없습니다.");
        }

        // 2. 멤버십 및 권한 검증
        MemberRoleResponse memberRole = teamServiceClient.getMemberRole(teamId, userId);

        if (!"ADMIN".equals(memberRole.getRole())) {
            throw new SecurityException("문서 업로드는 ADMIN 권한이 필요합니다.");
        }

        // 3. 암호화 데이터(Blob) 변환 및 생성
        byte[] blobData = Base64.getDecoder().decode(req.getEncryptedBlob());

        // 같은 이름의 파일이 있는지 확인
        Optional<Document> existingDoc = documentRepository.findByTeamIdAndOriginalFileName(teamId, req.getFileName());

        if (existingDoc.isPresent()) {
            // [Case A] 이미 존재함 -> 업데이트 (Version Up)
            Document document = existingDoc.get();
            EncryptedData data = document.getEncryptedData();

            data.updateContent(blobData);

        } else {
            // [Case B] 없음 -> 신규 생성 (Version 1)
            Document document = Document.builder()
                    .documentId(UUID.randomUUID().toString())
                    .teamId(teamId)
                    .uploaderId(userId)
                    .originalFileName(req.getFileName())
                    .fileType(req.getFileType())
                    .build();

            EncryptedData encryptedData = EncryptedData.builder()
                    .dataId(UUID.randomUUID().toString())
                    .document(document)
                    .encryptedBlob(blobData)
                    .build();

            document.setEncryptedData(encryptedData);
            documentRepository.save(document);
        }
    }

    // 문서 목록 조회
    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocuments(String teamId, String userId) {
        // 팀 존재 확인
        if (!teamServiceClient.teamExists(teamId)) {
            throw new IllegalArgumentException("저장소를 찾을 수 없습니다.");
        }

        // 멤버십 검증 (ADMIN과 MEMBER 모두 조회 가능)
        if (!teamServiceClient.memberExists(teamId, userId)) {
            throw new SecurityException("저장소 멤버가 아닙니다.");
        }

        return documentRepository.findAllByTeamId(teamId).stream()
                .map(DocumentResponse::from)
                .collect(Collectors.toList());
    }

    // 클라이언트 호환 업로드 (POST /api/documents)
    @Transactional
    public Long uploadDocumentFromClient(String repositoryId, String userId, String content) {
        // 1. 팀 존재 확인
        if (!teamServiceClient.teamExists(repositoryId)) {
            throw new IllegalArgumentException("저장소를 찾을 수 없습니다.");
        }

        // 2. 멤버십 검증
        teamServiceClient.getMemberRole(repositoryId, userId);

        // 3. 암호화된 콘텐츠를 바이너리로 변환
        byte[] blobData = Base64.getDecoder().decode(content);

        // 4. 문서 생성
        Document document = Document.builder()
                .documentId(UUID.randomUUID().toString())
                .teamId(repositoryId)
                .uploaderId(userId)
                .originalFileName("uploaded_document")
                .fileType("encrypted")
                .build();

        EncryptedData encryptedData = EncryptedData.builder()
                .dataId(UUID.randomUUID().toString())
                .document(document)
                .encryptedBlob(blobData)
                .build();

        document.setEncryptedData(encryptedData);
        Document saved = documentRepository.save(document);

        return saved.getId();
    }

    // 데이터 다운로드 (Long id 기반 - 클라이언트 호환)
    @Transactional(readOnly = true)
    public EncryptedDataResponse getDataById(Long id, String requesterId) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("문서를 찾을 수 없습니다."));

        checkDocumentPermission(document, requesterId);

        EncryptedData data = encryptedDataRepository.findByDocument(document)
                .orElseThrow(() -> new IllegalArgumentException("데이터가 존재하지 않습니다."));

        return EncryptedDataResponse.from(data);
    }

    // 데이터 다운로드 (UUID 기반 - 내부 API용)
    @Transactional(readOnly = true)
    public EncryptedDataResponse getData(String docId, String requesterId) {
        Document document = documentRepository.findByDocumentId(docId)
                .orElseThrow(() -> new IllegalArgumentException("문서를 찾을 수 없습니다."));

        checkDocumentPermission(document, requesterId);

        EncryptedData data = encryptedDataRepository.findByDocument(document)
                .orElseThrow(() -> new IllegalArgumentException("데이터가 존재하지 않습니다."));

        return EncryptedDataResponse.from(data);
    }

    // 문서 접근 권한 체크 (User 또는 EdgeNode)
    private void checkDocumentPermission(Document document, String requesterId) {
        boolean hasPermission = false;

        // 1. 사람(User)인지 확인
        try {
            if (authServiceClient.userExists(requesterId)) {
                hasPermission = teamServiceClient.memberExists(document.getTeamId(), requesterId);
            }
        } catch (Exception e) {
            // User 조회 실패 시 무시
        }

        // 2. 로봇(EdgeNode)인지 확인
        if (!hasPermission) {
            try {
                String edgeNodeTeamId = teamServiceClient.getEdgeNodeTeamId(requesterId);
                hasPermission = edgeNodeTeamId.equals(document.getTeamId());
            } catch (Exception e) {
                // EdgeNode 조회 실패 시 무시
            }
        }

        if (!hasPermission) {
            throw new SecurityException("접근 권한이 없습니다 (멤버 아님).");
        }
    }

    // 문서 삭제
    @Transactional
    public void deleteDocument(String docId, String userId) {
        Document document = documentRepository.findByDocumentId(docId)
                .orElseThrow(() -> new IllegalArgumentException("문서를 찾을 수 없습니다."));

        boolean isUploader = document.getUploaderId().equals(userId);

        // 멤버십 및 권한 확인
        MemberRoleResponse memberRole = teamServiceClient.getMemberRole(document.getTeamId(), userId);

        boolean isAdmin = "ADMIN".equals(memberRole.getRole());

        if (!isUploader && !isAdmin) {
            throw new SecurityException("삭제 권한이 없습니다.");
        }

        // 연관된 청크도 논리적 삭제 처리
        List<VectorChunk> chunks = vectorChunkRepository.findByDocumentId(docId);
        chunks.forEach(chunk -> chunk.markAsDeleted());

        documentRepository.delete(document);
    }
}

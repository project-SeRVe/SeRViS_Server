package horizon.SeRVe.service;

import horizon.SeRVe.dto.document.*;
import horizon.SeRVe.entity.*;
import horizon.SeRVe.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final EncryptedDataRepository encryptedDataRepository;
    private final TeamRepoRepository teamRepoRepository;
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;

    /**
     * [Modified] 기존 uploadDocument 메서드를 수정
     * - 파라미터 변경: 개별 인자 -> DTO (UploadDocumentRequest)
     * - 로직 변경: 단일 저장 -> Document(메타) + EncryptedData(바이너리) 분리 저장
     */
    @Transactional
    public void uploadDocument(String repoId, String userId, UploadDocumentRequest req) {
        // 1. 저장소 및 유저 조회
        TeamRepository repo = teamRepoRepository.findByRepoId(repoId)
                .orElseThrow(() -> new IllegalArgumentException("저장소를 찾을 수 없습니다."));

        User uploader = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 2. 메타데이터(Document) 생성
        Document document = Document.builder()
                .documentId(UUID.randomUUID().toString()) // UUID 생성
                .teamRepository(repo)
                .uploader(uploader)
                .originalFileName(req.getFileName())
                .fileType(req.getFileType())
                .build();

        // 3. 암호화 데이터(Blob) 변환 및 생성
        byte[] blobData = Base64.getDecoder().decode(req.getEncryptedBlob());

        EncryptedData encryptedData = EncryptedData.builder()
                .dataId(UUID.randomUUID().toString())
                .document(document)
                .encryptedBlob(blobData)
                .version(1)
                .build();

        // 4. 연결 및 저장 (Cascade)
        document.setEncryptedData(encryptedData);
        documentRepository.save(document);
    }

    // 문서 목록 조회
    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocuments(String repoId) {
        TeamRepository repo = teamRepoRepository.findByRepoId(repoId)
                .orElseThrow(() -> new IllegalArgumentException("저장소를 찾을 수 없습니다."));

        return documentRepository.findAllByTeamRepository(repo).stream()
                .map(DocumentResponse::from)
                .collect(Collectors.toList());
    }

    // 데이터 다운로드 (기존 getDocument -> getData로 역할 구체화)
    @Transactional(readOnly = true)
    public EncryptedDataResponse getData(String docId, String userId) {
        Document document = documentRepository.findByDocumentId(docId)
                .orElseThrow(() -> new IllegalArgumentException("문서를 찾을 수 없습니다."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 권한 체크
        if (!memberRepository.existsByTeamRepositoryAndUser(document.getTeamRepository(), user)) {
            throw new SecurityException("해당 저장소의 멤버만 문서를 다운로드할 수 있습니다.");
        }

        EncryptedData data = encryptedDataRepository.findByDocument(document)
                .orElseThrow(() -> new IllegalArgumentException("데이터가 존재하지 않습니다."));

        return EncryptedDataResponse.from(data);
    }

    // 문서 삭제
    @Transactional
    public void deleteDocument(String docId, String userId) {
        Document document = documentRepository.findByDocumentId(docId)
                .orElseThrow(() -> new IllegalArgumentException("문서를 찾을 수 없습니다."));

        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        boolean isUploader = document.getUploader().getUserId().equals(userId);

        RepositoryMember memberInfo = memberRepository.findByTeamRepositoryAndUser(document.getTeamRepository(), requester)
                .orElseThrow(() -> new SecurityException("저장소 멤버가 아닙니다."));

        boolean isAdmin = memberInfo.getRole() == Role.ADMIN;

        if (!isUploader && !isAdmin) {
            throw new SecurityException("삭제 권한이 없습니다.");
        }

        documentRepository.delete(document);
    }
}
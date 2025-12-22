package horizon.SeRVe.service;

import horizon.SeRVe.dto.document.*;
import horizon.SeRVe.entity.*;
import horizon.SeRVe.repository.*;
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
    private final TeamRepository teamRepository; // 기존: TeamRepoRepository
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final EdgeNodeRepository edgeNodeRepository;

    /**
     * [Modified] 기존 uploadDocument 메서드를 수정
     * - 파라미터 변경: 개별 인자 -> DTO (UploadDocumentRequest)
     * - 로직 변경: 단일 저장 -> Document(메타) + EncryptedData(바이너리) 분리 저장
     */
    @Transactional
    public void uploadDocument(String teamId, String userId, UploadDocumentRequest req) {
        // 1. 저장소 및 유저 조회
        // 기존: findByRepoId → findByTeamId
        Team team = teamRepository.findByTeamId(teamId)
                .orElseThrow(() -> new IllegalArgumentException("저장소를 찾을 수 없습니다."));

        User uploader = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));


        // 2. 암호화 데이터(Blob) 변환 및 생성
        byte[] blobData = Base64.getDecoder().decode(req.getEncryptedBlob());

        // 같은 이름의 파일이 있는지 확인
        Optional<Document> existingDoc = documentRepository.findByTeamAndOriginalFileName(team, req.getFileName());

        if (existingDoc.isPresent()) {
            // [Case A] 이미 존재함 -> 업데이트 (Version Up)
            Document document = existingDoc.get();
            EncryptedData data = document.getEncryptedData();

            data.updateContent(blobData);

        } else {
            // [Case B] 없음 -> 신규 생성 (Version 1)
            Document document = Document.builder()
                    .documentId(UUID.randomUUID().toString())
                    .team(team)
                    .uploader(uploader)
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
    public List<DocumentResponse> getDocuments(String teamId) {
        // 기존: findByRepoId → findByTeamId
        Team team = teamRepository.findByTeamId(teamId)
                .orElseThrow(() -> new IllegalArgumentException("저장소를 찾을 수 없습니다."));

        // 기존: findAllByTeamRepository → findAllByTeam
        return documentRepository.findAllByTeam(team).stream()
                .map(DocumentResponse::from)
                .collect(Collectors.toList());
    }

    // 데이터 다운로드 (기존 getDocument -> getData로 역할 구체화)
    @Transactional(readOnly = true)
    public EncryptedDataResponse getData(String docId, String requesterId) { // requesterId는 userId 또는 nodeId
        Document document = documentRepository.findByDocumentId(docId)
                .orElseThrow(() -> new IllegalArgumentException("문서를 찾을 수 없습니다."));

        // [권한 체크 로직 변경]
        boolean hasPermission = false;

        // 1. 사람(User)인지 확인
        if (userRepository.existsById(requesterId)) {
            User user = userRepository.findById(requesterId).get();
            // 기존 멤버십 체크
            hasPermission = memberRepository.existsByTeamAndUser(document.getTeam(), user);
        }
        // 2. 로봇(EdgeNode)인지 확인
        else if (edgeNodeRepository.existsById(requesterId)) {
            EdgeNode robot = edgeNodeRepository.findById(requesterId).get();
            // 로봇은 '소속 팀'이 문서의 팀과 같으면 권한 O
            hasPermission = robot.getTeam().equals(document.getTeam());
        }

        if (!hasPermission) {
            throw new SecurityException("접근 권한이 없습니다 (멤버 아님).");
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

        // 기존: document.getTeamRepository() → document.getTeam()
        RepositoryMember memberInfo = memberRepository.findByTeamAndUser(document.getTeam(), requester)
                .orElseThrow(() -> new SecurityException("저장소 멤버가 아닙니다."));

        boolean isAdmin = memberInfo.getRole() == Role.ADMIN;

        if (!isUploader && !isAdmin) {
            throw new SecurityException("삭제 권한이 없습니다.");
        }

        documentRepository.delete(document);
    }
}

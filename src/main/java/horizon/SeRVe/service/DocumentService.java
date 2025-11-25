package horizon.SeRVe.service;

import horizon.SeRVe.entity.Document;
import horizon.SeRVe.entity.TeamRepository;
import horizon.SeRVe.repository.DocumentRepository;
import horizon.SeRVe.repository.TeamRepoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final TeamRepoRepository teamRepoRepository;

    @Transactional
    public Long uploadDocument(Long repoId, String fileName, String encryptedContent, String ownerId) {
        // 1. 저장소 찾기
        TeamRepository repo = teamRepoRepository.findById(repoId)
                .orElseThrow(() -> new IllegalArgumentException("저장소를 찾을 수 없습니다."));

        // 2. 문서 엔티티 생성 (암호화된 상태 그대로 저장)
        Document doc = new Document(repo, fileName, encryptedContent, ownerId);

        // 3. 저장
        Document saved = documentRepository.save(doc);
        return saved.getId();
    }
    public Document getDocument(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("해당 문서를 찾을 수 없습니다. ID=" + documentId));
    }
}
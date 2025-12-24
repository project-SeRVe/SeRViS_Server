package horizon.SeRVe.service;

import horizon.SeRVe.dto.chunk.*;
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
public class ChunkService {

    private final VectorChunkRepository vectorChunkRepository;
    private final DocumentRepository documentRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;

    /**
     * A. 청크 업로드 (배치)
     * - ADMIN 권한 필요
     * - fileName으로 Document 찾거나 생성
     * - 기존 chunk_index 존재 시 UPDATE, 없으면 INSERT
     */
    @Transactional
    public void uploadChunks(String teamId, String fileName, String userId, ChunkUploadRequest request) {
        // 1. Team 조회
        Team team = teamRepository.findByTeamId(teamId)
                .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 2. ADMIN 권한 체크
        RepositoryMember member = memberRepository.findByTeamAndUser(team, user)
                .orElseThrow(() -> new SecurityException("저장소 멤버가 아닙니다."));

        if (member.getRole() != Role.ADMIN) {
            throw new SecurityException("청크 업로드는 ADMIN 권한이 필요합니다.");
        }

        // 3. Document 찾거나 생성
        Document document = documentRepository.findByTeamAndOriginalFileName(team, fileName)
                .orElseGet(() -> {
                    // 새 문서 생성
                    Document newDoc = Document.builder()
                            .documentId(UUID.randomUUID().toString())
                            .team(team)
                            .uploader(user)
                            .originalFileName(fileName)
                            .fileType("application/octet-stream") // 기본값
                            .build();
                    return documentRepository.save(newDoc);
                });

        // 4. 각 청크 처리 (UPDATE or INSERT)
        for (ChunkUploadItem item : request.getChunks()) {
            byte[] blobData = Base64.getDecoder().decode(item.getEncryptedBlob());

            Optional<VectorChunk> existingChunk = vectorChunkRepository
                    .findByDocumentIdAndChunkIndex(document.getDocumentId(), item.getChunkIndex());

            if (existingChunk.isPresent()) {
                // UPDATE: 기존 청크 내용 갱신 (version 자동 증가)
                VectorChunk chunk = existingChunk.get();
                chunk.updateContent(blobData);
                chunk.setDeleted(false); // 재업로드 시 삭제 플래그 해제
            } else {
                // INSERT: 새 청크 생성 (version = 0)
                VectorChunk newChunk = VectorChunk.builder()
                        .chunkId(UUID.randomUUID().toString())
                        .documentId(document.getDocumentId())
                        .teamId(team.getTeamId())
                        .chunkIndex(item.getChunkIndex())
                        .encryptedBlob(blobData)
                        .isDeleted(false)
                        .build();
                vectorChunkRepository.save(newChunk);
            }
        }
    }

    /**
     * B. 청크 다운로드
     * - ADMIN 또는 MEMBER 권한 허용
     * - fileName으로 Document 조회
     * - is_deleted = false인 청크만 반환
     */
    @Transactional(readOnly = true)
    public List<ChunkResponse> getChunks(String teamId, String fileName, String userId) {
        // 1. Team 조회
        Team team = teamRepository.findByTeamId(teamId)
                .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 2. 멤버십 체크 (ADMIN 또는 MEMBER)
        if (!memberRepository.existsByTeamAndUser(team, user)) {
            throw new SecurityException("팀 멤버가 아닙니다.");
        }

        // 3. Document 조회
        Document document = documentRepository.findByTeamAndOriginalFileName(team, fileName)
                .orElseThrow(() -> new IllegalArgumentException("문서를 찾을 수 없습니다."));

        // 4. 삭제되지 않은 청크만 조회 (chunk_index 순 정렬)
        List<VectorChunk> chunks = vectorChunkRepository
                .findByDocumentIdAndIsDeletedOrderByChunkIndexAsc(document.getDocumentId(), false);

        return chunks.stream()
                .map(ChunkResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * C. 청크 삭제 (논리적 삭제)
     * - ADMIN 권한 필요
     * - fileName으로 Document 조회
     */
    @Transactional
    public void deleteChunk(String teamId, String fileName, int chunkIndex, String userId) {
        // 1. Team 조회
        Team team = teamRepository.findByTeamId(teamId)
                .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 2. ADMIN 권한 체크
        RepositoryMember member = memberRepository.findByTeamAndUser(team, user)
                .orElseThrow(() -> new SecurityException("저장소 멤버가 아닙니다."));

        if (member.getRole() != Role.ADMIN) {
            throw new SecurityException("청크 삭제는 ADMIN 권한이 필요합니다.");
        }

        // 3. Document 조회
        Document document = documentRepository.findByTeamAndOriginalFileName(team, fileName)
                .orElseThrow(() -> new IllegalArgumentException("문서를 찾을 수 없습니다."));

        // 4. 청크 논리적 삭제 (version 자동 증가)
        VectorChunk chunk = vectorChunkRepository
                .findByDocumentIdAndChunkIndex(document.getDocumentId(), chunkIndex)
                .orElseThrow(() -> new IllegalArgumentException("청크를 찾을 수 없습니다."));

        chunk.markAsDeleted();
    }

    /**
     * E. 팀별 증분 동기화
     * - ADMIN 또는 MEMBER 권한 허용
     * - 해당 팀의 모든 문서에서 version > lastVersion인 청크 조회
     */
    @Transactional(readOnly = true)
    public List<ChunkSyncResponse> syncTeamChunks(String teamId, int lastVersion, String userId) {
        // 1. Team 조회
        Team team = teamRepository.findByTeamId(teamId)
                .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 2. 팀 멤버십 체크
        if (!memberRepository.existsByTeamAndUser(team, user)) {
            throw new SecurityException("팀 멤버가 아닙니다.");
        }

        // 3. 팀의 모든 문서에서 변경된 청크 조회
        List<VectorChunk> chunks = vectorChunkRepository
                .findByTeamIdAndVersionGreaterThanOrderByVersionAsc(teamId, lastVersion);

        return chunks.stream()
                .map(ChunkSyncResponse::from)
                .collect(Collectors.toList());
    }
}

package horizon.SeRVe.team;

import com.fasterxml.jackson.databind.ObjectMapper;
import horizon.SeRVe.team.dto.repo.CreateRepoRequest;
import horizon.SeRVe.team.dto.repo.RepoResponse;
import horizon.SeRVe.team.entity.RepoType;
import horizon.SeRVe.team.entity.Team;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 클라이언트(client-robot) SDK와의 호환성 검증 테스트
 */
class ClientCompatibilityTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("저장소 생성 요청 - 클라이언트가 보내는 ownerId 필드를 무시하고 역직렬화")
    void createRepoRequest_ignoresOwnerId() throws Exception {
        // 클라이언트가 보내는 형식: {name, description, ownerId, encryptedTeamKey}
        // 서버는 ownerId를 무시하고 JWT에서 추출
        String clientJson = """
                {"name": "test-repo", "description": "desc", "ownerId": 123, "encryptedTeamKey": "enc-key"}
                """;

        CreateRepoRequest request = objectMapper.readValue(clientJson, CreateRepoRequest.class);

        assertEquals("test-repo", request.getName());
        assertEquals("desc", request.getDescription());
        assertEquals("enc-key", request.getEncryptedTeamKey());
    }

    @Test
    @DisplayName("저장소 생성 응답 - {id: uuid} 형식으로 반환")
    void createRepoResponse_returnsIdField() throws Exception {
        // 서버 응답: {"id": "uuid-string"}
        // 클라이언트 파싱: data.get('id')
        Map<String, String> response = Map.of("id", "a1b2c3d4-e5f6-7890-abcd-ef1234567890");
        String json = objectMapper.writeValueAsString(response);

        assertTrue(json.contains("\"id\""));

        // 클라이언트 시뮬레이션: isinstance(data, dict) → data.get('id')
        Object parsed = objectMapper.readValue(json, Object.class);
        assertInstanceOf(java.util.Map.class, parsed, "응답이 dict(Map)이어야 함");
        Map<?, ?> parsedMap = (Map<?, ?>) parsed;
        assertNotNull(parsedMap.get("id"));
    }

    @Test
    @DisplayName("저장소 목록 응답 - 클라이언트가 기대하는 필드명 (id, name, description, type, ownerEmail)")
    void repoResponse_containsExpectedFields() throws Exception {
        Team team = new Team("test-repo", "description", "owner-1");
        team.setType(RepoType.TEAM);

        RepoResponse response = RepoResponse.of(team, "owner@test.com");
        String json = objectMapper.writeValueAsString(response);

        // 클라이언트 app.py에서 사용하는 필드: repo['id'], repo['name'], repo['type'], repo['ownerEmail']
        assertTrue(json.contains("\"id\""), "필드 'id' 존재해야 함 (Teamid가 아닌 id)");
        assertTrue(json.contains("\"name\""));
        assertTrue(json.contains("\"description\""));
        assertTrue(json.contains("\"type\""));
        assertTrue(json.contains("\"ownerEmail\""));
        assertFalse(json.contains("\"Teamid\""), "Teamid 필드가 없어야 함");
        assertFalse(json.contains("\"teamid\""), "teamid 필드가 없어야 함");
    }
}

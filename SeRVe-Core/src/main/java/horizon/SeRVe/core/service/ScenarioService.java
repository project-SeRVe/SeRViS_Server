package horizon.SeRVe.core.service;

import horizon.SeRVe.core.dto.demo.DemoResponse;
import horizon.SeRVe.core.dto.scenario.ScenarioCreateRequest;
import horizon.SeRVe.core.dto.scenario.ScenarioResponse;
import horizon.SeRVe.core.entity.Scenario;
import horizon.SeRVe.core.repository.DemoRepository;
import horizon.SeRVe.core.repository.ScenarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScenarioService {

    private final ScenarioRepository scenarioRepository;
    private final DemoRepository demoRepository;

    // Scenario 등록: promptHash 기준 중복이면 기존 반환
    @Transactional
    public ScenarioResponse createScenario(ScenarioCreateRequest request) {
        String hash = sha256(request.getPromptText());

        return scenarioRepository.findByPromptHash(hash)
                .map(ScenarioResponse::from)
                .orElseGet(() -> {
                    Scenario scenario = Scenario.builder()
                            .scenarioId(UUID.randomUUID().toString())
                            .promptText(request.getPromptText())
                            .promptHash(hash)
                            .build();
                    return ScenarioResponse.from(scenarioRepository.save(scenario));
                });
    }

    // 전체 Scenario 목록 조회
    @Transactional(readOnly = true)
    public List<ScenarioResponse> getScenarios() {
        return scenarioRepository.findAll().stream()
                .map(ScenarioResponse::from)
                .collect(Collectors.toList());
    }

    // Scenario 단건 조회
    @Transactional(readOnly = true)
    public ScenarioResponse getScenario(String scenarioId) {
        return scenarioRepository.findById(scenarioId)
                .map(ScenarioResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("Scenario를 찾을 수 없습니다."));
    }

    // Scenario별 Demo 목록 조회 (새 Demo 엔티티)
    @Transactional(readOnly = true)
    public List<DemoResponse> getDemosByScenario(String scenarioId) {
        if (!scenarioRepository.existsById(scenarioId)) {
            throw new IllegalArgumentException("Scenario를 찾을 수 없습니다.");
        }
        return demoRepository.findByScenario_ScenarioId(scenarioId).stream()
                .map(DemoResponse::from)
                .collect(Collectors.toList());
    }

    // Demo 단건 조회 (새 Demo 엔티티)
    @Transactional(readOnly = true)
    public DemoResponse getDemo(String demoId) {
        return demoRepository.findById(demoId)
                .map(DemoResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("Demo를 찾을 수 없습니다."));
    }

    // SHA-256 해시 생성 (promptHash 중복 방지용)
    private String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }
}

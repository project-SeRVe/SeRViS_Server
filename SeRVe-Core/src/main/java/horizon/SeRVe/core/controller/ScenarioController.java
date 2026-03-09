package horizon.SeRVe.core.controller;

import horizon.SeRVe.core.dto.demo.DemoResponse;
import horizon.SeRVe.core.dto.scenario.ScenarioCreateRequest;
import horizon.SeRVe.core.dto.scenario.ScenarioResponse;
import horizon.SeRVe.core.service.ScenarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ScenarioController {

    private final ScenarioService scenarioService;

    // Scenario 등록 (promptHash 중복이면 기존 반환)
    @PostMapping("/api/scenarios")
    public ResponseEntity<ScenarioResponse> createScenario(
            @RequestBody ScenarioCreateRequest request) {
        return ResponseEntity.ok(scenarioService.createScenario(request));
    }

    // 전체 Scenario 목록 조회
    @GetMapping("/api/scenarios")
    public ResponseEntity<List<ScenarioResponse>> getScenarios() {
        return ResponseEntity.ok(scenarioService.getScenarios());
    }

    // Scenario 단건 조회
    @GetMapping("/api/scenarios/{scenarioId}")
    public ResponseEntity<ScenarioResponse> getScenario(
            @PathVariable String scenarioId) {
        return ResponseEntity.ok(scenarioService.getScenario(scenarioId));
    }

    // Scenario별 Demo 목록 조회
    @GetMapping("/api/scenarios/{scenarioId}/demos")
    public ResponseEntity<List<DemoResponse>> getDemosByScenario(
            @PathVariable String scenarioId) {
        return ResponseEntity.ok(scenarioService.getDemosByScenario(scenarioId));
    }
}

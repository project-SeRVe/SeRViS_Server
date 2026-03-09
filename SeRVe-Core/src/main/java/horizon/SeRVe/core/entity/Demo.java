package horizon.SeRVe.core.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "demos", indexes = {
    @Index(name = "idx_demos_scenario", columnList = "scenario_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Demo {

    @Id
    @Column(name = "demo_id", length = 64)
    private String demoId; // UUID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", nullable = false)
    private Scenario scenario;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private String status = "APPROVED"; // 서버에는 approved된 것만 올라옴

    @Column(name = "num_steps")
    private Integer numSteps;

    @Column(name = "state_dim")
    private Integer stateDim;

    @Column(name = "action_dim")
    private Integer actionDim;

    @Column(name = "image_h")
    private Integer imageH;

    @Column(name = "image_w")
    private Integer imageW;

    @Column(name = "embed_dim")
    private Integer embedDim;

    @Column(name = "embed_model_id", length = 100)
    private String embedModelId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}

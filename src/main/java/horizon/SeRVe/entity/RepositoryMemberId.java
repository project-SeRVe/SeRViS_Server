package horizon.SeRVe.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepositoryMemberId implements Serializable {
    private String teamId; // 기존: repoId
    private String userId;

    //private String teamRepository; // TeamRepository의 repoId 타입과 일치해야 함
    //private String user;
}

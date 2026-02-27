package horizon.SeRVe.core.dto.demo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DemoUploadRequest {
    private String fileName;
    private List<DemoUploadItem> demos;
}

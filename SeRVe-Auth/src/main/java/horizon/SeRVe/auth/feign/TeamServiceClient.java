package horizon.SeRVe.auth.feign;

import horizon.SeRVe.common.dto.feign.EdgeNodeAuthResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "serve-team", url = "${service.team.url}")
public interface TeamServiceClient {

    @GetMapping("/internal/edge-nodes/by-serial/{serialNumber}")
    EdgeNodeAuthResponse getEdgeNodeBySerial(@PathVariable String serialNumber);
}

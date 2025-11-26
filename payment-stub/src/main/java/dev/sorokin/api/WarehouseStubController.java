package dev.sorokin.api;

import dev.sorokin.api.warehouse.CalculatePricingRequestDto;
import dev.sorokin.api.warehouse.CalculatePricingResponseDto;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController("/warehouse")
@AllArgsConstructor
public class WarehouseStubController {

    @PostMapping("/calculate-price")
    public ResponseEntity<CalculatePricingResponseDto> capturePayment(
            @RequestBody CalculatePricingRequestDto calculatePricingRequest
    ) {
        return ResponseEntity.ok().build();
    }

}

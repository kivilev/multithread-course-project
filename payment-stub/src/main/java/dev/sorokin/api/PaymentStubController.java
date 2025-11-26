package dev.sorokin.api;

import dev.sorokin.api.payment.AuthorizePaymentRequestDto;
import dev.sorokin.api.payment.AuthorizePaymentResponseDto;
import dev.sorokin.api.payment.CapturePaymentRequestDto;
import dev.sorokin.api.payment.CapturePaymentResponseDto;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController("/payment")
@AllArgsConstructor
public class PaymentStubController {

    @PostMapping("/authorize")
    public ResponseEntity<AuthorizePaymentResponseDto> authorizePayment(
            @RequestBody AuthorizePaymentRequestDto authorizePaymentRequest
    ) {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/capture")
    public ResponseEntity<CapturePaymentResponseDto> capturePayment(
            @RequestBody CapturePaymentRequestDto capturePaymentRequest
    ) {
        return ResponseEntity.ok().build();
    }

}

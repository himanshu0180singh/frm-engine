package com.company.frm.controller;

import com.company.frm.dto.FraudCheckResponse;
import com.company.frm.dto.TransactionRequest;
import com.company.frm.engine.FraudEvaluationPipeline;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fraud")
@RequiredArgsConstructor
@Slf4j
public class FraudCheckController {

    private final FraudEvaluationPipeline pipeline;

    @PostMapping("/check")
    public ResponseEntity<FraudCheckResponse> check(@Valid @RequestBody TransactionRequest request) {
        log.info("Fraud check request received: txnId={}, channel={}, sender={}",
                request.getTransactionId(), request.getChannel(), request.getSenderId());
        FraudCheckResponse response = pipeline.evaluate(request);
        log.info("Fraud check result: txnId={}, decision={}, score={}",
                request.getTransactionId(), response.getDecision(), response.getRiskScore());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("FRM Engine is running");
    }
}

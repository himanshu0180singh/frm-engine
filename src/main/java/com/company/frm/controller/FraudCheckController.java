package com.company.frm.controller;

import com.company.frm.dto.FraudCheckResponse;
import com.company.frm.dto.TransactionRequest;
import com.company.frm.engine.FraudEvaluationPipeline;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/fraud")
@RequiredArgsConstructor
public class FraudCheckController {

    private final FraudEvaluationPipeline pipeline;

    @PostMapping("/check")
    public ResponseEntity<FraudCheckResponse> checkTransaction(
            @Valid @RequestBody TransactionRequest request) {
        FraudCheckResponse response = pipeline.evaluate(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("FRM Engine is running");
    }
}

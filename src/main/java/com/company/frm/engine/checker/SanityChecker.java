package com.company.frm.engine.checker;

import com.company.frm.dto.TransactionRequest;
import org.springframework.stereotype.Component;

@Component
public class SanityChecker {

    /**
     * Performs basic sanity validation on the transaction.
     *
     * @return error message if validation fails, null if all checks pass
     */
    public String check(TransactionRequest request) {
        if (request.getTransactionId() == null || request.getTransactionId().isBlank()) {
            return "transactionId is missing";
        }
        if (request.getSenderId() == null || request.getSenderId().isBlank()) {
            return "senderId is missing";
        }
        if (request.getReceiverId() == null || request.getReceiverId().isBlank()) {
            return "receiverId is missing";
        }
        if (request.getAmount() == null || request.getAmount().signum() <= 0) {
            return "amount must be positive";
        }
        if (request.getChannel() == null) {
            return "channel is missing";
        }
        if (request.getTxnTime() == null) {
            return "txnTime is missing";
        }
        if (request.getSenderId().equals(request.getReceiverId())) {
            return "self-transfer detected: senderId equals receiverId";
        }
        return null;
    }
}

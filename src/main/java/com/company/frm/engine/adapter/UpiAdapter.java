package com.company.frm.engine.adapter;

import com.company.frm.dto.TransactionRequest;
import com.company.frm.enums.Channel;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Adapts UPI XML / JSON payloads to canonical TransactionRequest DTO.
 */
@Component
public class UpiAdapter implements ChannelAdapter {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Override
    public Channel getChannel() {
        return Channel.UPI;
    }

    @Override
    public TransactionRequest adapt(Map<String, String> raw) {
        return TransactionRequest.builder()
                .transactionId(raw.get("txnRefId"))
                .channel(Channel.UPI)
                .senderId(raw.get("payerVpa"))
                .receiverId(raw.get("payeeVpa"))
                .amount(new BigDecimal(raw.getOrDefault("amount", "0")))
                .currency(raw.getOrDefault("currency", "INR"))
                .deviceId(raw.get("deviceId"))
                .senderIp(raw.get("senderIp"))
                .upiVpa(raw.get("payerVpa"))
                .collectRequest(Boolean.parseBoolean(raw.getOrDefault("collectRequest", "false")))
                .txnTime(raw.containsKey("txnTime")
                        ? LocalDateTime.parse(raw.get("txnTime"), FORMATTER)
                        : LocalDateTime.now())
                .build();
    }
}

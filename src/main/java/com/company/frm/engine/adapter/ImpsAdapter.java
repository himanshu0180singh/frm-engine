package com.company.frm.engine.adapter;

import com.company.frm.dto.TransactionRequest;
import com.company.frm.enums.Channel;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Adapts IMPS JSON payloads to canonical TransactionRequest DTO.
 */
@Component
public class ImpsAdapter implements ChannelAdapter {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Override
    public Channel getChannel() {
        return Channel.IMPS;
    }

    @Override
    public TransactionRequest adapt(Map<String, String> raw) {
        return TransactionRequest.builder()
                .transactionId(raw.get("rrn"))
                .channel(Channel.IMPS)
                .senderId(raw.get("debitAccount"))
                .receiverId(raw.get("creditAccount"))
                .amount(new BigDecimal(raw.getOrDefault("amount", "0")))
                .currency(raw.getOrDefault("currency", "INR"))
                .deviceId(raw.get("deviceId"))
                .senderIp(raw.get("senderIp"))
                .txnTime(raw.containsKey("txnTime")
                        ? LocalDateTime.parse(raw.get("txnTime"), FORMATTER)
                        : LocalDateTime.now())
                .build();
    }
}

package com.company.frm.engine.adapter;

import com.company.frm.dto.TransactionRequest;
import com.company.frm.enums.Channel;

import java.util.Map;

/**
 * Channel Adapter interface — each channel implementation normalises
 * its native payload into a canonical {@link TransactionRequest} DTO.
 */
public interface ChannelAdapter {

    Channel getChannel();

    /**
     * Converts a raw channel-specific payload (represented as a key-value map)
     * into a normalised TransactionRequest DTO understood by the FRM core engine.
     */
    TransactionRequest adapt(Map<String, String> rawPayload);
}

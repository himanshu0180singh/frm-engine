package com.company.frm.service;

import com.company.frm.dto.TransactionRequest;
import com.company.frm.entity.FrmCustomerProfile;
import com.company.frm.repository.FrmCustomerProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerProfileService {

    private final FrmCustomerProfileRepository profileRepository;

    public Optional<FrmCustomerProfile> getProfile(String customerId) {
        return profileRepository.findById(customerId);
    }

    @Async
    @Transactional
    public void updateProfileAfterTxn(TransactionRequest request) {
        try {
            FrmCustomerProfile profile = profileRepository.findById(request.getSenderId())
                    .orElseGet(() -> FrmCustomerProfile.builder()
                            .customerId(request.getSenderId())
                            .avgTxnAmount(BigDecimal.ZERO)
                            .totalTxnCount(0L)
                            .riskTier("LOW")
                            .createdAt(LocalDateTime.now())
                            .build());

            long newCount = profile.getTotalTxnCount() + 1;
            BigDecimal newAvg = profile.getAvgTxnAmount()
                    .multiply(BigDecimal.valueOf(profile.getTotalTxnCount()))
                    .add(request.getAmount())
                    .divide(BigDecimal.valueOf(newCount), 2, RoundingMode.HALF_UP);

            profile.setTotalTxnCount(newCount);
            profile.setAvgTxnAmount(newAvg);
            profile.setLastTxnDate(request.getTxnTime());
            if (request.getSenderCity() != null) {
                profile.setLastTxnCity(request.getSenderCity());
            }
            if (request.getDeviceId() != null) {
                profile.setLastDeviceId(request.getDeviceId());
            }
            profile.setUpdatedAt(LocalDateTime.now());

            profileRepository.save(profile);
        } catch (Exception e) {
            log.error("Failed to update customer profile for {}: {}", request.getSenderId(), e.getMessage());
        }
    }
}

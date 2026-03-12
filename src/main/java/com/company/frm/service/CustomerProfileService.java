package com.company.frm.service;

import com.company.frm.entity.FrmCustomerProfile;
import com.company.frm.repository.CustomerProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerProfileService {

    private final CustomerProfileRepository customerProfileRepository;

    public Optional<FrmCustomerProfile> findByCustomerId(String customerId) {
        return customerProfileRepository.findByCustomerId(customerId);
    }

    public FrmCustomerProfile save(FrmCustomerProfile profile) {
        return customerProfileRepository.save(profile);
    }
}

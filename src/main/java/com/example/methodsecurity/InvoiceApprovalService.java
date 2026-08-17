package com.example.methodsecurity;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class InvoiceApprovalService {

    private final ApprovalRepository repository;

    public InvoiceApprovalService(ApprovalRepository repository) {
        this.repository = repository;
    }

    @PreAuthorize("hasRole('FINANCE')")
    public void approve(String invoiceId) {
        repository.approve(invoiceId);
    }
}

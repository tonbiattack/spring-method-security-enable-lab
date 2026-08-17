package com.example.methodsecurity;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.stereotype.Repository;

@Repository
public class ApprovalRepository {

    private final Set<String> approvedInvoiceIds = new LinkedHashSet<>();

    public synchronized void approve(String invoiceId) {
        approvedInvoiceIds.add(invoiceId);
    }

    public synchronized boolean isApproved(String invoiceId) {
        return approvedInvoiceIds.contains(invoiceId);
    }

    public synchronized void clear() {
        approvedInvoiceIds.clear();
    }

    public synchronized Set<String> approvedIds() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(approvedInvoiceIds));
    }
}

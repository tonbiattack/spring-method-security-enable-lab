package com.example.methodsecurity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest
class InvoiceApprovalServiceIntegrationTest {

    @Autowired
    private InvoiceApprovalService approvalService;

    @Autowired
    private ApprovalRepository repository;

    @BeforeEach
    void setUp() {
        repository.clear();
    }

    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void 経理権限を持たない利用者は請求書を承認できない() {
        Throwable thrown = catchThrowable(() -> approvalService.approve("invoice-001"));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(thrown)
                    .as("経理権限がなければAccessDeniedExceptionで拒否されること")
                    .isInstanceOf(AccessDeniedException.class);
            softly.assertThat(repository.isApproved("invoice-001"))
                    .as("拒否された操作は承認済み状態を残さないこと")
                    .isFalse();
        });
    }

    @Test
    @WithMockUser(username = "finance", roles = "FINANCE")
    void 経理権限を持つ利用者は請求書を承認できる() {
        approvalService.approve("invoice-002");

        assertThat(repository.isApproved("invoice-002"))
                .as("許可された操作は承認済み状態を残すこと")
                .isTrue();
    }
}

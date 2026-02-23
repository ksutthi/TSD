package com.tsd.adapter.output.policy

import com.tsd.core.model.PlatformAction
import com.tsd.core.model.UserIdentity
import com.tsd.core.port.output.PolicyEnginePort
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
@Profile("local", "test")
class LocalMockPolicyAdapter : PolicyEnginePort {
    private val log = LoggerFactory.getLogger(LocalMockPolicyAdapter::class.java)

    override fun isAuthorized(user: UserIdentity, action: PlatformAction, amount: BigDecimal): Boolean {
        log.info("⚖️ [POLICY ENGINE] Evaluating -> User: ${user.userId} | Action: $action | Amount: $amount")

        val isApproved = when (action) {
            PlatformAction.INITIATE_TRANSFER -> {
                // Makers can initiate, but only up to their limit
                user.hasRole("MAKER") && amount <= user.approvalLimit
            }
            PlatformAction.APPROVE_TRANSFER -> {
                // Checkers and Head of Ops can approve, up to their limit
                (user.hasRole("CHECKER") || user.hasRole("HEAD_OF_OPERATIONS")) && amount <= user.approvalLimit
            }
            PlatformAction.VIEW_GOD_MODE_PORTFOLIO -> {
                user.hasRole("TSD_ADMIN") || user.hasRole("SYSTEM")
            }
            PlatformAction.EXECUTE_CORPORATE_ACTION -> {
                user.hasRole("HEAD_OF_OPERATIONS") || user.hasRole("SYSTEM")
            }
        }

        if (isApproved) {
            log.info("✅ [POLICY ENGINE] Result: PERMIT")
        } else {
            log.warn("❌ [POLICY ENGINE] Result: DENY (Insufficient Role or Limit Exceeded)")
        }

        return isApproved
    }
}
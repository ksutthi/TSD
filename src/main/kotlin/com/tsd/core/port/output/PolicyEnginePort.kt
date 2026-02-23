package com.tsd.core.port.output

import com.tsd.core.model.PlatformAction
import com.tsd.core.model.UserIdentity
import java.math.BigDecimal

interface PolicyEnginePort {
    /**
     * Asks the external Policy Engine (OPA/Cerbos) if the user can perform this action.
     * @param user The current identity (from SecurityContextPort)
     * @param action The action they want to perform
     * @param amount Optional financial amount to check against approval limits
     */
    fun isAuthorized(user: UserIdentity, action: PlatformAction, amount: BigDecimal = BigDecimal.ZERO): Boolean
}
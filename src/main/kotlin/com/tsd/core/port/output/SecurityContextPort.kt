package com.tsd.core.port.output

import com.tsd.core.model.UserIdentity

interface SecurityContextPort {
    fun getCurrentUser(): UserIdentity
}
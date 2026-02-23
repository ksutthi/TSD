package com.tsd.core.port.output

import com.tsd.core.model.IdentityAttribute

interface IdentityAttributePort {
    fun findGinIdByTypeAndValue(type: String, value: String): Long?
    fun save(attribute: IdentityAttribute): IdentityAttribute
}
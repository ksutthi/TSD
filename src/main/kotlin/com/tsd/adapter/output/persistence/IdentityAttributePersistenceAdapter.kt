package com.tsd.adapter.output.persistence

import com.tsd.core.model.IdentityAttribute
import com.tsd.core.port.output.IdentityAttributePort
import org.springframework.stereotype.Component

@Component
class IdentityAttributePersistenceAdapter(
    // Injecting the framework-specific Spring Data repository
    private val repository: IdentityAttributeJpaRepository
) : IdentityAttributePort {

    // Fulfilling the pure Kotlin interface contract
    override fun findGinIdByTypeAndValue(type: String, value: String): Long? {
        return repository.findGinIdByTypeAndValue(type, value)
    }

    override fun save(attribute: IdentityAttribute): IdentityAttribute {
        return repository.save(attribute)
    }
}
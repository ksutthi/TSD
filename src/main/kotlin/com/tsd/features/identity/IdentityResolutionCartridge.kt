package com.tsd.features.identity

import com.tsd.core.model.GlobalInvestorRegistry
import com.tsd.core.model.IdentityAttribute
import com.tsd.core.port.out.GlobalInvestorRegistryPort
import com.tsd.core.port.out.IdentityAttributePort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * The Input payload representing a raw, unstructured row from a Book Closing file
 * or the Registration_Queue.
 */
data class IncomingHolderRecord(
    val queueId: Int,
    val firstName: String,
    val lastName: String,
    val taxId: String?,
    val idType: String, // e.g., "CITIZEN_ID", "PASSPORT"
    val idValue: String,
    val country: String
)

@Component
class IdentityResolutionCartridge(
    // 🟢 HEXAGONAL PORTS: The cartridge only speaks to pure Kotlin interfaces.
    private val identityAttributePort: IdentityAttributePort,
    private val globalInvestorRegistryPort: GlobalInvestorRegistryPort
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    /**
     * Executes the configured Probabilistic Matching Rules to find or create the Tier 1 Anchor (GIN_ID).
     */
    @Transactional
    fun resolveIdentity(record: IncomingHolderRecord): Long {
        log.info("Running Identity Resolution for Queue ID: ${record.queueId}")

        // 🟢 RULE 1: High Confidence Match (Exact ID + Type)
        val exactMatch = findByExactId(record.idType, record.idValue)
        if (exactMatch != null) {
            log.info("Match found via Rule 1 (Exact ID) -> GIN_ID: $exactMatch")
            return exactMatch
        }

        // 🟢 RULE 2: Medium Confidence Match (Tax ID + Name)
        if (record.taxId != null) {
            val taxMatch = findByTaxAndName(record.taxId, record.firstName, record.lastName)
            if (taxMatch != null) {
                log.info("Match found via Rule 2 (Tax + Name) -> GIN_ID: $taxMatch")
                return taxMatch
            }
        }

        // 🟢 NO MATCH FOUND: Create a brand new Global Investor (Tier 1 Anchor)
        log.info("No probabilistic match found for Queue ID: ${record.queueId}. Generating new Global Investor ID.")
        return createNewGlobalInvestor(record)
    }

    private fun findByExactId(idType: String, idValue: String): Long? {
        return identityAttributePort.findGinIdByTypeAndValue(idType, idValue)
    }

    private fun findByTaxAndName(taxId: String, firstName: String, lastName: String): Long? {
        // Query the identity attributes via the outbound port
        val matchByTax = identityAttributePort.findGinIdByTypeAndValue("TAX_ID", taxId)
        val matchByName = identityAttributePort.findGinIdByTypeAndValue("FIRST_NAME", firstName)

        // Only return if BOTH vectors point to the exact same human being
        return if (matchByTax != null && matchByTax == matchByName) matchByTax else null
    }

    private fun createNewGlobalInvestor(record: IncomingHolderRecord): Long {
        // 1. Insert into Global_Investor_Registry
        val newRegistry = GlobalInvestorRegistry(
            ginCode = generateGinCode(),
            createdDate = LocalDateTime.now()
        )
        val savedRegistry = globalInvestorRegistryPort.save(newRegistry)
        val newGinId = savedRegistry.ginId ?: throw IllegalStateException("Failed to generate GIN_ID")

        // 2. Insert the matching vectors into Identity_Attributes for future runs
        identityAttributePort.save(IdentityAttribute(globalInvestorId = newGinId, attributeType = "FIRST_NAME", attributeValue = record.firstName))
        identityAttributePort.save(IdentityAttribute(globalInvestorId = newGinId, attributeType = "LAST_NAME", attributeValue = record.lastName))
        identityAttributePort.save(IdentityAttribute(globalInvestorId = newGinId, attributeType = record.idType, attributeValue = record.idValue))

        if (record.taxId != null) {
            identityAttributePort.save(IdentityAttribute(globalInvestorId = newGinId, attributeType = "TAX_ID", attributeValue = record.taxId))
        }

        return newGinId
    }

    private fun generateGinCode(): String {
        // Generates the public-facing ID (e.g., "GID-827364")
        return "GID-${System.currentTimeMillis().toString().takeLast(6)}"
    }
}
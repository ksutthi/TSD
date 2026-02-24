package com.tsd.platform.cartridge

import com.tsd.platform.engine.core.WorkflowSuspendedException
import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.spi.Cartridge
import com.tsd.platform.spi.ExchangePacket
import com.tsd.platform.spi.ExecutionContext
import org.springframework.stereotype.Component

@Component
class RequireApprovalCartridge : Cartridge {

    override val id: String = "Require_Approval"
    override val version: String = "1.0"
    override val priority: Int = 100

    // 🟢 Required by your SPI
    override fun initialize(context: ExecutionContext) {
        // No startup setup needed for this cartridge
    }

    override fun execute(packet: ExchangePacket, context: ExecutionContext) {
        // We check the payload to see if the Checker has stamped it as approved
        val approvalStatus = packet.data["APPROVAL_STATUS"]?.toString()

        if (approvalStatus == "APPROVED") {
            println(EngineAnsi.GREEN + "      ✅ [Require_Approval] Human Approval Token Verified! Proceeding..." + EngineAnsi.RESET)
            return
        }

        // If not approved, we throw our special exception to halt the engine
        println(EngineAnsi.YELLOW + "      🛑 [Require_Approval] Execution halted. Awaiting external Maker/Checker approval..." + EngineAnsi.RESET)
        throw WorkflowSuspendedException("Transaction paused. Awaiting human approval.")
    }

    override fun compensate(packet: ExchangePacket, context: ExecutionContext) {
        // A pause requires no data compensation, so this remains empty
    }

    // 🟢 Required by your SPI
    override fun shutdown() {
        // No teardown needed for this cartridge
    }
}
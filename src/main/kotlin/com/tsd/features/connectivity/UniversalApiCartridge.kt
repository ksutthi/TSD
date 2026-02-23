package com.tsd.features.connectivity

import com.tsd.platform.engine.state.KernelContext
import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.spi.Cartridge
import com.tsd.platform.spi.ExchangePacket
import com.tsd.platform.spi.ExecutionContext
import com.tsd.platform.spi.UniversalHttpPort
import org.springframework.stereotype.Component

@Component
class UniversalApiCartridge(
    private val httpPort: UniversalHttpPort
) : Cartridge {

    override val id: String = "Universal_API_Connector"
    override val version: String = "1.0"

    // 🟢 1. Set a standard priority for external integrations
    override val priority: Int = 50

    // 🟢 2. Fulfill the lifecycle requirements safely
    override fun initialize(context: ExecutionContext) {
        // Nothing needed on startup
    }

    override fun shutdown() {
        // Nothing needed on shutdown
    }

    // 🟢 3. Use the officially required ExecutionContext signature
    override fun execute(packet: ExchangePacket, context: ExecutionContext) {
        val kernel = context as? KernelContext

        // 🟢 Pull directly from the KernelContext where the Engine injected it!
        val targetUrl = kernel?.getObject<String>("API_URL")
        val httpMethod = kernel?.getObject<String>("API_METHOD") ?: "POST"

        if (targetUrl == null) {
            println(EngineAnsi.YELLOW + "      ⚠️ [UniversalApiCartridge] Missing 'API_URL' in Rule Config. Skipping." + EngineAnsi.RESET)
            return
        }

        println(EngineAnsi.GREEN + "      🎯 Config Found! Firing HTTP Request..." + EngineAnsi.RESET)

        // Fire the hardened network call
        httpPort.executeCall(packet.id, targetUrl, httpMethod, packet.data)

        packet.data["Last_API_Status"] = "SUCCESS"
    }

    // 🟢 4. Updated the parameter here as well to match the interface shape
    override fun compensate(packet: ExchangePacket, context: ExecutionContext) {
        val kernel = context as? KernelContext
        val targetUrl = kernel?.getObject<String>("API_URL") ?: "Unknown URL"

        println("      ↩️ [Saga-API] Initiating Compensation/Rollback for previously successful call to $targetUrl...")
        packet.data.remove("Last_API_Status")
    }
}
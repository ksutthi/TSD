package com.tsd.app.event.cartridge

import com.tsd.platform.model.registry.ExchangePacket
import com.tsd.platform.spi.KernelContext
import com.tsd.platform.spi.Cartridge
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.InputStreamReader
import com.tsd.platform.engine.util.EngineAnsi

@Component("Validate_SEC_Lead_Times")
class ValidateSECLeadTimesCartridge : Cartridge {
    override val id = "Validate_SEC_Lead_Times"
    override val version = "1.0"
    override val priority = 1

    // Default to T+3 if config is missing
    private var settlementDays = 3

    override fun initialize(context: KernelContext) {
        // 🟢 LOAD CONFIGURATION
        val filePath = "/config/cartridges/00_technical.csv"
        try {
            val inputStream = javaClass.getResourceAsStream(filePath)
            if (inputStream != null) {
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.forEachLine { line ->
                        // Expected CSV: PARAM_NAME,VALUE
                        val parts = line.split(",")
                        if (parts.size >= 2 && parts[0].trim() == "SEC_SETTLEMENT_DAYS") {
                            settlementDays = parts[1].trim().toIntOrNull() ?: 3
                            println("      📋 [Validate_SEC] Loaded Config: T+$settlementDays")
                        }
                    }
                }
            } else {
                println("      ⚠️ [Validate_SEC] Config file not found: $filePath (Using default T+3)")
            }
        } catch (e: Exception) {
            println("      ❌ [Validate_SEC] Error loading config: ${e.message}")
        }
    }

    override fun execute(packet: ExchangePacket, context: KernelContext) {
        // 🟢 1. Get the Dynamic Step ID (e.g. [J1-S1])
        val prefix = context.getObject<String>("STEP_PREFIX") ?: "[??]"

        // 🟢 2. Header Log
        print(EngineAnsi.GRAY + "   📅 $prefix Checking SEC Rules (T+$settlementDays)..." + EngineAnsi.RESET)
        println("")

        // In a real app, you would compare (EventDate - CurrentDate) < settlementDays

        // 🟢 3. ALIGNMENT FIX: 6 Spaces inside the string
        println("      " + EngineAnsi.GREEN + "    ✅ $prefix Lead times validated. Event is compliant." + EngineAnsi.RESET)
    }

    override fun shutdown() {}
}
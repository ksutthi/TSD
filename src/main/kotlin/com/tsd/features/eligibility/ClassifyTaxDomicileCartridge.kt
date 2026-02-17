package com.tsd.features.eligibility

import com.tsd.platform.spi.ExchangePacket
import com.tsd.platform.spi.ExecutionContext
import com.tsd.platform.spi.Cartridge
import com.tsd.platform.engine.util.EngineAnsi
import org.springframework.stereotype.Component

@Component("Classify_Tax_Domicile")
class ClassifyTaxDomicileCartridge : Cartridge {

    override val id: String = "Classify_Tax_Domicile"
    override val version: String = "1.0"
    override val priority: Int = 30

    override fun initialize(context: ExecutionContext) {}

    override fun execute(packet: ExchangePacket, context: ExecutionContext) {
        // 🟢 1. Get Dynamic Prefix
        val prefix = context.getObject<String>("STEP_PREFIX") ?: "[??]"

        // 🟢 2. Header Log
        print(EngineAnsi.GRAY + "   🌍 $prefix Analyzing Tax Domicile..." + EngineAnsi.RESET)
        println("")

        val profile = packet.data["Tax_Profile"] as? String ?: "Standard_Individual"

        if (profile == "Standard_Individual") {
            // 🟢 FIX: Dynamic Prefix + 6-Space Alignment
            println("      " + EngineAnsi.GREEN + "    ✅ $prefix Classification: LOCAL_RESIDENT (TH)" + EngineAnsi.RESET)
            packet.data["Domicile_Code"] = "TH"
            packet.data["WHT_Rate_Code"] = "Standard_10"
        } else {
            // 🟢 FIX: Dynamic Prefix + 6-Space Alignment
            println("      " + EngineAnsi.YELLOW + "      ✈️ $prefix Classification: FOREIGN_ENTITY (Non-Resident)" + EngineAnsi.RESET)
            packet.data["Domicile_Code"] = "FOREIGN"
            packet.data["WHT_Rate_Code"] = "Treaty_Rate"
        }
    }

    override fun shutdown() {}
}
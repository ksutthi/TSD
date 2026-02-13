package com.tsd.app.test.cartridge

import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.model.registry.ExchangePacket
import com.tsd.platform.spi.Cartridge
import com.tsd.platform.spi.KernelContext
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger

/**
 * 🧪 CHAOS MONKEY CARTRIDGE
 * Used for Stress Testing the Engine's strategies (Parallel, Retry, Async).
 *
 * ⚠️ SAFEGUARD: This component ONLY loads in "dev" or "test" profiles.
 * It will effectively vanish in a Production build.
 */
@Component("Chaos_Monkey")
@Profile("dev", "test", "default")
class ChaosCartridge : Cartridge {

    override val id = "Chaos_Monkey"
    override val version = "0.0.1"
    override val priority = 999

    // Counter to track retries across different threads
    private val crashCounter = AtomicInteger(0)

    override fun execute(packet: ExchangePacket, context: KernelContext) {
        // 1. Read the Config injected by the Engine
        val mode = context.getObject<String>("CHAOS_MODE") ?: "NORMAL"
        val prefix = context.getObject<String>("STEP_PREFIX") ?: "[CHAOS]"

        // 2. Execute the requested Chaos Strategy
        when (mode) {
            "SLEEP" -> {
                // 💤 Simulate a slow external system (e.g. Bank API latency)
                println(EngineAnsi.CYAN + "      💤 $prefix Monkey is sleeping for 2 seconds..." + EngineAnsi.RESET)
                try {
                    Thread.sleep(2000)
                } catch (_: InterruptedException) { // 🟢 FIX: Use '_' to ignore
                    Thread.currentThread().interrupt()
                }
                println(EngineAnsi.GREEN + "      ⏰ $prefix Monkey woke up!" + EngineAnsi.RESET)
            }

            "CRASH_ONCE" -> {
                // 💥 Simulate a network glitch that fails once, then works
                val attempt = crashCounter.incrementAndGet()
                if (attempt == 1) {
                    println(EngineAnsi.RED + "      💥 $prefix Monkey TRIPPED! (Attempt $attempt)" + EngineAnsi.RESET)
                    throw RuntimeException("Ouch! I fell (Simulated Crash).")
                } else {
                    println(EngineAnsi.GREEN + "      ✅ $prefix Monkey walked steadily. (Attempt $attempt)" + EngineAnsi.RESET)
                    crashCounter.set(0) // Reset for next time
                }
            }

            "ASYNC_Log" -> {
                // 👻 Simulate a background task (e.g. Email notification)
                // 🟢 FIX: Use MAGENTA instead of PURPLE
                println(EngineAnsi.MAGENTA + "      👻 $prefix Monkey is running in background..." + EngineAnsi.RESET)
                try {
                    Thread.sleep(1000)
                } catch (_: InterruptedException) { } // 🟢 FIX: Use '_' to ignore
                println(EngineAnsi.MAGENTA + "      👻 $prefix Monkey finished background work!" + EngineAnsi.RESET)
            }

            else -> {
                println("      🐵 Monkey does nothing (Mode: $mode).")
            }
        }
    }

    override fun initialize(context: KernelContext) {
        println(EngineAnsi.YELLOW + "      🐵 [Chaos] Chaos Monkey loaded and ready to break things." + EngineAnsi.RESET)
    }

    override fun shutdown() {
        println("      🐵 [Chaos] Chaos Monkey Going Home.")
    }
}
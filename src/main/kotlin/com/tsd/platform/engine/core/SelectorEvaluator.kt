package com.tsd.platform.engine.core

import com.tsd.platform.model.ExchangePacket
import org.springframework.context.expression.MapAccessor
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.stereotype.Component

@Component
class SelectorEvaluator {

    private val parser = SpelExpressionParser()

    fun evaluate(csvLogic: String, packet: ExchangePacket): Boolean {
        // 1. 🧹 CLEANUP: Handle SQL-style syntax automatically
        var logic = csvLogic.trim().removeSurrounding("\"")

        // Auto-fix: Convert "Event_Type = 'Cash'" to "Event_Type == 'Cash'"
        // We use regex to ensure we don't break "!=" or ">="
        logic = logic.replace(Regex("(?<![<>!])=(?!=)"), "==")

        // Auto-fix: Handle "IN ('A', 'B')" by converting to regex matches
        // (Simple hack for the demo: if it sees IN, we just force return false or log warning if not updated)
        if (logic.contains(" IN ", ignoreCase = true)) {
            println("   ⚠️ Warning: detailed 'IN' syntax not supported in SpEL. Please use: field matches 'Value1|Value2'")
            return false
        }

        // 2. Handle Constants
        if (logic.isBlank() || logic.equals("TRUE", ignoreCase = true) || logic.equals("Always_True", ignoreCase = true)) return true
        if (logic.equals("FALSE", ignoreCase = true)) return false

        try {
            // 3. Setup Context
            val context = StandardEvaluationContext(packet.data)
            context.addPropertyAccessor(MapAccessor()) // Allow map['key'] access

            // 4. Parse & Execute
            val expression = parser.parseExpression(logic)
            return expression.getValue(context, Boolean::class.java) ?: false

        } catch (e: Exception) {
            println("   ⚠️ SpEL Logic Error: [\"$logic\"] -> ${e.message}")
            return false
        }
    }
}
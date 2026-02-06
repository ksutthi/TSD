package com.tsd.app.functional_area.stub.web

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/external/services")
class RemoteTaxService {

    @PostMapping("/tax-calculator")
    fun calculateTax(@RequestBody payload: Map<String, Any>): Map<String, Any> {
        println("📡 [MICROSERVICE] Received Request. Calculating Tax...")

        // Simulate network latency
        try {
            Thread.sleep(100)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }

        return mapOf(
            "Tax_Amount" to 700.00,
            "WHT_Rate" to "7%",
            "Status" to "Processed_Remote"
        )
    }
}
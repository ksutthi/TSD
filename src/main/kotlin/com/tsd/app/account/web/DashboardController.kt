package com.tsd.app.account.web

import com.tsd.app.account.web.PortfolioController
import com.tsd.platform.spi.Cartridge
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class DashboardController(
    private val cartridges: List<Cartridge>,
    private val portfolioController: PortfolioController
) {

    @GetMapping("/")
    fun dashboard(model: Model): String {
        // 1. ENGINE STATUS (The Skeleton)
        val sortedList = cartridges.sortedBy { it.priority }
        model.addAttribute("cartridges", sortedList)
        model.addAttribute("status", "System Online 🟢")

        // 2. GOD VIEW DATA
        // We simulate a "God Mode" call to get all data for the UI
        val allAssets = portfolioController.getMyPortfolio(null, null, true)
        model.addAttribute("assets", allAssets)

        return "dashboard"
    }
}
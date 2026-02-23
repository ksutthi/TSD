package com.tsd.core.model

/**
 * PURE DOMAIN: The distinct actions that require authorization in the platform.
 */
enum class PlatformAction {
    INITIATE_TRANSFER,
    APPROVE_TRANSFER,
    VIEW_GOD_MODE_PORTFOLIO,
    EXECUTE_CORPORATE_ACTION
}
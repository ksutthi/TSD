package com.tsd.platform.engine.core

/**
 * The specific signal that tells the Engine to sleep instead of crash.
 */
class WorkflowSuspendedException(message: String) : RuntimeException(message)
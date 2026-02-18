package com.tsd.core.model

/**
 * The Official Lifecycle of a TSD Transaction.
 * This Enum enforces the State Machine rules.
 */
enum class WorkflowStatus {

    // 1. The Beginning
    INIT {
        override fun allowedTransitions(): Set<WorkflowStatus> = setOf(RUNNING, REJECTED)
    },

    // 🟢 2. NEW: The Engine is executing (Persistence Layer needs this)
    RUNNING {
        override fun allowedTransitions(): Set<WorkflowStatus> = setOf(PENDING, CLEARED, SETTLED, FAILED)
    },

    // 3. Waiting for Async / Consensus
    PENDING {
        override fun allowedTransitions(): Set<WorkflowStatus> = setOf(RUNNING, CLEARED, REJECTED, FAILED)
    },

    // 4. Step Success (Audit Trail)
    CLEARED {
        override fun allowedTransitions(): Set<WorkflowStatus> = setOf(RUNNING, SETTLED, FAILED)
    },

    // 🏁 5. Terminal States (Success)
    SETTLED {
        override fun allowedTransitions(): Set<WorkflowStatus> = emptySet()
    },

    REJECTED {
        override fun allowedTransitions(): Set<WorkflowStatus> = emptySet()
    },

    // 6. Failure & Recovery
    FAILED {
        // We can go to COMPENSATED (Rollback success) or PENDING (Retry)
        override fun allowedTransitions(): Set<WorkflowStatus> = setOf(COMPENSATED, PENDING)
    },

    // 🟢 7. NEW: Saga Rollback Completed
    COMPENSATED {
        override fun allowedTransitions(): Set<WorkflowStatus> = emptySet()
    };

    /**
     * Defines which states are valid next steps from THIS state.
     */
    abstract fun allowedTransitions(): Set<WorkflowStatus>

    /**
     * The Guardrail: Checks if moving to [target] is legal.
     * @throws IllegalStateException if the move is forbidden.
     */
    fun verifyTransition(target: WorkflowStatus) {
        if (target == this) return // Staying same is always okay

        if (!allowedTransitions().contains(target)) {
            // 🟢 Improved Error Message for Debugging
            throw IllegalStateException(
                "❌ ILLEGAL STATE MOVE: Cannot go from '$name' to '$target'. " +
                        "Valid moves are: ${allowedTransitions()}"
            )
        }
    }
}
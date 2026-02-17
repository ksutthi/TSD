package com.tsd.core.model

/**
 * The Official Lifecycle of a TSD Transaction.
 * This Enum enforces the State Machine rules.
 */
enum class WorkflowStatus {
    // 1. The Beginning
    INIT {
        override fun allowedTransitions(): Set<WorkflowStatus> = setOf(PENDING, REJECTED)
    },

    // 2. The Processing State (Waiting for votes/checks)
    PENDING {
        override fun allowedTransitions(): Set<WorkflowStatus> = setOf(CLEARED, REJECTED, FAILED)
    },

    // 3. The Happy Path (Ready for Settlement)
    CLEARED {
        override fun allowedTransitions(): Set<WorkflowStatus> = setOf(SETTLED, FAILED)
    },

    // 4. Terminal States (The End)
    SETTLED {
        override fun allowedTransitions(): Set<WorkflowStatus> = emptySet()
    },

    REJECTED {
        override fun allowedTransitions(): Set<WorkflowStatus> = emptySet()
    },

    FAILED {
        override fun allowedTransitions(): Set<WorkflowStatus> = setOf(PENDING) // Allow retry?
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
            throw IllegalStateException(
                "❌ ILLEGAL STATE MOVE: Cannot go from $name to $target. " +
                        "Valid moves are: ${allowedTransitions()}"
            )
        }
    }
}
-- The Vault: Stores the state of every workflow
CREATE TABLE IF NOT EXISTS workflow_job (
                                            job_id          VARCHAR(50) PRIMARY KEY,  -- "TXN-5-xxxx"
    workflow_id     VARCHAR(20) NOT NULL,     -- "N"
    current_step    VARCHAR(50) NOT NULL,     -- "N1-S1-C2"
    status          VARCHAR(20) NOT NULL,     -- "PAUSED", "RUNNING"
    payload         TEXT,                     -- The JSON Context {"Net_Amount": 25000000}
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Index for fast lookup of "Stuck" jobs
CREATE INDEX IF NOT EXISTS idx_workflow_status ON workflow_job(status);
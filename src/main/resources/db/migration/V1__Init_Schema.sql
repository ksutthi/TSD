-- 1. Account Balances Table (The "Ledger")
CREATE TABLE AccountBalances (
                                 Account_ID BIGINT PRIMARY KEY,
                                 Quantity DECIMAL(38, 2) DEFAULT 0.00
);

-- 2. Audit Log Table (For Compliance)
CREATE TABLE AuditLog (
                          id UNIQUEIDENTIFIER DEFAULT newid() PRIMARY KEY,
                          job_id VARCHAR(255),
                          step_name VARCHAR(255),
                          message NVARCHAR(MAX),
                          status VARCHAR(50),
                          timestamp DATETIME DEFAULT GETDATE()
);

-- 3. Initial Seed Data (The "Billionaire" and "Standard Users")
INSERT INTO AccountBalances (Account_ID, Quantity) VALUES (1, 10000.00);
INSERT INTO AccountBalances (Account_ID, Quantity) VALUES (2, 20000.00);
INSERT INTO AccountBalances (Account_ID, Quantity) VALUES (3, 30000.00);
INSERT INTO AccountBalances (Account_ID, Quantity) VALUES (4, 40000.00);
INSERT INTO AccountBalances (Account_ID, Quantity) VALUES (5, 30000000.00); -- The Billionaire

-- 4. Workflow Job Table (Updated with all required columns)
-- If the table already exists, you might need to DROP it first or just replace the create statement if you are using H2 memory.
DROP TABLE IF EXISTS WORKFLOW_JOB;

CREATE TABLE WORKFLOW_JOB (
                              job_id VARCHAR(255) NOT NULL PRIMARY KEY,
                              workflow_id VARCHAR(255) NOT NULL,  -- New
                              current_step VARCHAR(255) NOT NULL, -- New
                              status VARCHAR(50) NOT NULL,
                              payload NVARCHAR(MAX)               -- New (For JSON)
);
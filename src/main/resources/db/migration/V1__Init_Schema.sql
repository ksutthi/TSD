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
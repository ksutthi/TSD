-- =============================================
-- Schema Update V2: Persistence & Recovery
-- Date: 2026-02-17
-- Description: Creates the Journal table for Lazarus Recovery
-- =============================================

IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[Workflow_Journal]') AND type in (N'U'))
    BEGIN
        CREATE TABLE dbo.Workflow_Journal
        (
            Job_ID     varchar(50) not null primary key,
            Payload    varchar(max),
            Status     varchar(20), -- INIT, PENDING, SETTLED, FAILED, RECOVERED
            Created_At datetime default getdate()
        );
        PRINT '✅ Table Workflow_Journal created successfully.';
    END
ELSE
    BEGIN
        PRINT '⚠️ Table Workflow_Journal already exists. Skipping.';
    END
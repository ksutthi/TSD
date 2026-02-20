-- =============================================
-- Schema Update V3: Enable Matrix Access Control (God View)
-- Description: Safely adds GIN_ID to transactional and identity tables.
-- =============================================

BEGIN TRANSACTION;
BEGIN TRY

    -- 1. Rename Investor_ID to GIN_ID in Accounts for naming consistency
    IF COL_LENGTH('dbo.Accounts', 'Investor_ID') IS NOT NULL
        BEGIN
            EXEC sp_rename 'dbo.Accounts.Investor_ID', 'GIN_ID', 'COLUMN';
            PRINT 'Renamed Investor_ID to GIN_ID in Accounts.';
        END

    -- 2. Add GIN_ID to AccountBalances to enable the "God View" Filter
    IF COL_LENGTH('dbo.AccountBalances', 'GIN_ID') IS NULL
        BEGIN
            ALTER TABLE dbo.AccountBalances ADD GIN_ID bigint NULL;
            PRINT 'Added GIN_ID to AccountBalances.';
        END

    -- 3. Standardize Identity and Ledger tables to use the BIGINT ID
    IF COL_LENGTH('dbo.Identity_Attributes', 'GIN_ID') IS NULL
        BEGIN
            ALTER TABLE dbo.Identity_Attributes ADD GIN_ID bigint NULL;
            PRINT 'Added GIN_ID to Identity_Attributes.';
        END

    IF COL_LENGTH('dbo.Identity_Core_Map', 'GIN_ID') IS NULL
        BEGIN
            ALTER TABLE dbo.Identity_Core_Map ADD GIN_ID bigint NULL;
            PRINT 'Added GIN_ID to Identity_Core_Map.';
        END

    IF COL_LENGTH('dbo.Payment_Ledger', 'GIN_ID') IS NULL
        BEGIN
            ALTER TABLE dbo.Payment_Ledger ADD GIN_ID bigint NULL;
            PRINT 'Added GIN_ID to Payment_Ledger.';
        END

    IF COL_LENGTH('dbo.Sanctions_Master', 'GIN_ID') IS NULL
        BEGIN
            ALTER TABLE dbo.Sanctions_Master ADD GIN_ID bigint NULL;
            PRINT 'Added GIN_ID to Sanctions_Master.';
        END

    COMMIT TRANSACTION;
    PRINT '✅ V3 Schema adjustments completed successfully.';

END TRY
BEGIN CATCH
    ROLLBACK TRANSACTION;
    PRINT '❌ ERROR OCCURRED. Rolling back changes.';
    PRINT ERROR_MESSAGE();
END CATCH;
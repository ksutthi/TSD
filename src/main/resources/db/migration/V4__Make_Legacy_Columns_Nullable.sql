-- =============================================
-- Schema Update V4: Relax Legacy Constraints
-- Description: Allows the old GIN string columns to be NULL
-- so the new Hexagonal engine can use the BIGINT GIN_ID exclusively.
-- =============================================

BEGIN TRANSACTION;
BEGIN TRY

    -- Relax Identity_Attributes
    IF COL_LENGTH('dbo.Identity_Attributes', 'GIN') IS NOT NULL
        BEGIN
            ALTER TABLE dbo.Identity_Attributes ALTER COLUMN GIN nvarchar(20) NULL;
            PRINT 'Made GIN nullable in Identity_Attributes.';
        END

    -- Relax Identity_Core_Map
    IF COL_LENGTH('dbo.Identity_Core_Map', 'GIN') IS NOT NULL
        BEGIN
            ALTER TABLE dbo.Identity_Core_Map ALTER COLUMN GIN nvarchar(20) NULL;
            PRINT 'Made GIN nullable in Identity_Core_Map.';
        END

    -- Relax Payment_Ledger
    IF COL_LENGTH('dbo.Payment_Ledger', 'GIN') IS NOT NULL
        BEGIN
            ALTER TABLE dbo.Payment_Ledger ALTER COLUMN GIN nvarchar(20) NULL;
            PRINT 'Made GIN nullable in Payment_Ledger.';
        END

    -- Relax Sanctions_Master
    IF COL_LENGTH('dbo.Sanctions_Master', 'GIN') IS NOT NULL
        BEGIN
            ALTER TABLE dbo.Sanctions_Master ALTER COLUMN GIN nvarchar(20) NULL;
            PRINT 'Made GIN nullable in Sanctions_Master.';
        END

    COMMIT TRANSACTION;
    PRINT '✅ V4 Schema adjustments completed successfully.';

END TRY
BEGIN CATCH
    ROLLBACK TRANSACTION;
    PRINT '❌ ERROR OCCURRED. Rolling back changes.';
    PRINT ERROR_MESSAGE();
END CATCH;
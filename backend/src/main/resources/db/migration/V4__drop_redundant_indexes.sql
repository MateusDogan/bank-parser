-- Os dois indices abaixo sao prefixo de indices compostos que ja existem:
--   idx_statements_organization    (organization_id)
--     coberto por idx_statements_org_uploaded  (organization_id, uploaded_at DESC)
--   idx_transactions_organization  (organization_id)
--     coberto por idx_transactions_org_date    (organization_id, transaction_date DESC)
--
-- O Postgres usa o composto para filtrar so por organization_id, entao manter
-- os dois significa pagar escrita em dobro a cada insert sem ganho de leitura.

DROP INDEX IF EXISTS idx_statements_organization;
DROP INDEX IF EXISTS idx_transactions_organization;

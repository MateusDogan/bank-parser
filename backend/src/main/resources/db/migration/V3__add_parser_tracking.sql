-- Rastreabilidade do parser: qual versao processou cada Statement, e se a
-- checagem de continuidade de saldo achou algo estranho nele.
--
-- Backfill de parser_version e seguro: StoneParser 1.0 e o unico parser que
-- ja existiu neste codigo, entao todo Statement ja persistido de fato foi
-- processado por ele. validation_flags NAO tem backfill -- recalcular exige
-- reler as Transactions de cada Statement, trabalho separado e opcional.

ALTER TABLE statements ADD COLUMN parser_version VARCHAR(10);
ALTER TABLE statements ADD COLUMN validation_flags TEXT;

UPDATE statements SET parser_version = '1.0' WHERE parser_version IS NULL;

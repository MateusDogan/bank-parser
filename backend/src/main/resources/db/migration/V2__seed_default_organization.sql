-- Organization padrao do escritorio. O sistema nao funciona sem pelo menos um
-- tenant, e ate a Fase 10 (SaaS publico) nao ha fluxo de cadastro de novos.
-- O UUID e fixo para que a aplicacao possa referencia-lo sem consultar o banco.
INSERT INTO organizations (id, name)
VALUES ('00000000-0000-0000-0000-000000000001', 'Escritorio')
ON CONFLICT (id) DO NOTHING;

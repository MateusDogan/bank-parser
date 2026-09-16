# PDFs "Golden" para Teste do Parser

Colocar aqui PDFs de extratos reais (ou anonimizados) usados como fixtures de teste, junto com o JSON de saída esperada correspondente.

Convenção de nomes:
```
extrato_stone_01.pdf
extrato_stone_01_expected.json
extrato_stone_02.pdf
extrato_stone_02_expected.json
```

**Atenção**: PDFs reais de clientes não devem ser commitados com dados sensíveis expostos — anonimizar nomes/CNPJs antes de adicionar aqui, ou usar PDFs sintéticos que reproduzam o mesmo layout.

O `.gitignore` do projeto bloqueia `*.pdf` por padrão, exceto os que estiverem em `src/test/resources/**` — confirmar essa exceção antes de adicionar fixtures aqui.

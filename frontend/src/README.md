# Frontend Source

> **Andaime.** Existe para exercitar a API enquanto o modelo de tela novo não
> chega, e será substituído na Fase 4 (ver `DEVELOPMENT_PLAN.md`). Não vale
> investir aqui além do mínimo para testar o backend.

React minimalista: upload de PDF e download de CSV, nada mais.
Paginação, filtros, dashboard e gráficos ficam para uma fase de expansão futura
(o `recharts` já está no `package.json` esperando por isso, mas não é usado ainda).

```
src/
├── main.jsx          # entry point, HashRouter (GitHub Pages nao suporta BrowserRouter sem config extra)
├── App.jsx            # layout + 2 rotas
├── pages/
│   ├── Upload.jsx      # cadastro rapido de cliente + upload de PDF
│   └── Statements.jsx  # lista de extratos + download CSV
├── services/
│   └── api.js          # unico ponto de chamadas HTTP (axios)
└── index.css
```

**`VITE_API_URL`**: variável de build que aponta pro backend. Ver `.env.example`.
Em dev local, não precisa configurar nada — o default já é `http://localhost:8080`.

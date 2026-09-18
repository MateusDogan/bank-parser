# Frontend Source

React minimalista: arrastar um PDF, enviar, baixar o CSV. Nada mais.

O visual segue o modelo feito no Lovable (header escuro, fundo bege pontilhado,
área de arraste tracejada, botão pill). As cores ficam em variáveis CSS no topo
do `index.css` — mexer no tema é mexer só lá.

```
src/
├── main.jsx           # entry point, HashRouter (GitHub Pages nao suporta BrowserRouter sem config extra)
├── App.jsx            # layout + 2 rotas
├── pages/
│   ├── Upload.jsx     # area de arraste + envio do PDF
│   └── Statements.jsx # lista de extratos + download CSV
├── services/
│   └── api.js         # unico ponto de chamadas HTTP (axios)
└── index.css
```

**`VITE_API_URL`**: variável de build que aponta pro backend. Ver `.env.example`.
Em dev local, não precisa configurar nada — o default já é `http://localhost:8080`.

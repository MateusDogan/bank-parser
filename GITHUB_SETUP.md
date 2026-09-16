# GitHub Setup: Actions + Pages

Workflows foram criados. Agora configure o repositório.

## ✅ GitHub Actions (Automático)

Os workflows em `.github/workflows/` já estão prontos:
- `frontend-deploy.yml` — Deploy automático do frontend em GitHub Pages
- `backend-test.yml` — Rodas testes do backend em todo push

GitHub Actions já está **habilitado por padrão**. Vá em:
- Repository → **Actions** → Verifique que os workflows aparecem lá

Pronto — Actions roda automaticamente em todo push.

---

## ⚠️ GitHub Pages (Manual — Precisa de Um Click)

Antes do workflow funcionar, você precisa **habilitar GitHub Pages uma vez**.

### Passo 1: Acesse Settings
1. Vá em **Settings** (engrenagem, canto superior direito do repo)
2. Menu lateral esquerdo: **Pages**

### Passo 2: Configure a Source
- **Build and deployment** → **Source**
- Mude de "Deploy from a branch" para **GitHub Actions**
- Salva

### Passo 3: Workflow Roda Automaticamente
Primeira vez que o `frontend-deploy.yml` roda (após Fase 4 quando existir `frontend/src/main.jsx`):
1. Constrói React (`npm run build`)
2. Deploy em GitHub Pages
3. Frontend fica vivo em: `https://MateusDogan.github.io/bank-parser/`

---

## 🔧 Configuração do Frontend no Workflow

**Atenção**: O workflow atual tem placeholder para `REACT_APP_API_URL`:
```yaml
env:
  REACT_APP_API_URL: https://seu-backend-url.com
```

Substitua pelo seu backend real quando souber:
- Lightsail: `https://seu-backend.lightnsail.com`
- VPS próprio: `https://seu-dominio.com`
- Local/intranet: `http://seu-ip:8080`

---

## ✓ Resumo: O Que Fazer Agora

1. **Workflows criados** ✅ (`.github/workflows/`)
2. **Vá em Settings → Pages** → mude para **GitHub Actions**
3. **Pronto** — Actions rodam automaticamente, Pages ativado

Quando Fase 4 estiver pronta (frontend + backend em main):
- `backend-test.yml` roda → testa
- `frontend-deploy.yml` roda → constrói e publica em GitHub Pages
- Sem nenhuma ação manual

---

## Troubleshooting

**Q: Workflow falha ao rodar?**
- Vá em **Actions** → veja o log do workflow que falhou
- Erro comum: API_URL errada no build — edite o workflow com a URL real do backend

**Q: GitHub Pages ainda diz "No Pages site found"?**
- Aguarde 1-2 min após primeira execução bem-sucedida do workflow
- Depois acesse `https://MateusDogan.github.io/bank-parser/`

**Q: Quero rodar frontend local em vez de GitHub Pages?**
- Isso é ok tbm — Phase 4 pode rodar localmente durante dev
- GitHub Pages é só pra quando quiser publicar (depois)

---

*Última atualização: 2026-09-16*

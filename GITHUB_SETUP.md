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
A cada push que toque `frontend/**`:
1. Constrói React com Vite (`npm run build`)
2. Deploy em GitHub Pages
3. Frontend fica vivo em: `https://MateusDogan.github.io/bank-parser/`

---

## 🔧 Apontar o Frontend para o Backend Real

O workflow usa a variável `VITE_API_URL` (Vite, não Create React App), lida de uma
**repository variable** — assim você não precisa editar o YAML quando o backend mudar de lugar:

```yaml
env:
  VITE_API_URL: ${{ vars.BACKEND_URL || 'http://localhost:8080' }}
```

**Configurar `BACKEND_URL`**:
1. Repository → **Settings** → **Secrets and variables** → **Actions** → aba **Variables**
2. **New repository variable**: nome `BACKEND_URL`, valor a URL real do backend
   - Lightsail/VPS: `https://seu-backend.com`
   - Intranet do escritório: **não funciona** — GitHub Pages é público na internet e não alcança
     um IP privado. Nesse caso, sirva o frontend do mesmo host do backend em vez de GitHub Pages.
3. Sem essa variável configurada, o build usa `http://localhost:8080` como fallback (só funciona
   se quem abrir a página também tiver o backend rodando localmente — ok para dev, não para uso real)

---

## ✓ Resumo: O Que Fazer Agora

1. **Workflows criados** ✅ (`.github/workflows/`)
2. **Vá em Settings → Pages** → mude para **GitHub Actions**
3. **Pronto** — Actions rodam automaticamente, Pages ativado

A partir de agora, todo push em `master`:
- `backend-test.yml` roda → testa (33+ testes)
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

# Learnings — Frontend
> Atualizado pelo agente frontend após cada retrospectiva.

## Design system (aprovado — não mudar sem alinhamento)

- Cor principal: azul #4a7fc4. Apoios pastéis com significado fixo:
  verde-menta = receita/positivo; coral = despesa/atenção; areia = aviso; lilás = destaque.
- Tipografia: Fraunces (títulos/valores) + Plus Jakarta Sans (corpo/UI).
- Modos claro e escuro via CSS variables. Ambos obrigatórios.
- Referência visual: `docs/design-system.html`.

## Padrões estabelecidos

- Módulos por feature: auth, transactions, dashboard, cards, budget, settlement.
- Serviços HTTP isolados por feature; nunca `HttpClient` direto no componente.
- Moeda em pt-br (R$, vírgula decimal). Datas: dd/MM/yyyy.
- Mensagens de erro da API exibidas em pt-br (já vêm assim do backend).
- Uma das usuárias não é técnica: fluxos simples, rótulos claros, poucos cliques.

## Aprendizados das retrospectivas

### Sprint 01 (2026-05-27)

**Angular Material 21 / MD3 — Theming:**
- `@use '@angular/material' as mat` deve ser a **primeira linha** do SCSS — antes de qualquer `@import url(...)` — ou o build falha com "use rules must be written before any other rules"
- Para dark mode: `body.dark-theme { @include mat.theme((color: (theme-type: dark, ...))) }` — aplicar classe no `document.body`, não no `:root`
- Paleta `mat.$azure-palette` é a mais próxima do azul #4a7fc4 do design system

**Angular 21 — Padrões modernos:**
- Guards funcionais: `export const authGuard: CanActivateFn = () => { ... }` — não usar classes com `implements CanActivate`
- Interceptors funcionais: `export const authInterceptor: HttpInterceptorFn = (req, next) => { ... }`
- Registrar interceptor: `provideHttpClient(withInterceptors([authInterceptor]))` no `app.config.ts`
- Signals para estado: `signal()`, `computed()`, `.asReadonly()` — padrão para AuthService e ThemeService

**ThemeService:**
- Detectar preferência do sistema com `window.matchMedia('(prefers-color-scheme: dark)').matches`
- Persistir em localStorage com chave prefixada (`gastos-theme`) para evitar colisão

**Lazy loading e code splitting:**
- Usar `loadChildren` com `() => import('./auth/auth.routes').then(m => m.authRoutes)` para módulo de auth
- Usar `loadComponent` para componentes isolados como o dashboard placeholder

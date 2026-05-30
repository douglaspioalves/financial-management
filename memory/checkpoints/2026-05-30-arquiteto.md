# Checkpoint Arquiteto — 2026-05-30 15:00

## Feito nesta sessão
- Leitura completa do estado do projeto após Sprint 07 (v1.0.0 concluída).
- Análise da stack frontend: Angular 21.2, Angular Material 21.2, builder @angular/build:application (esbuild), sem @angular/pwa instalado, sem service worker configurado.
- Produção do plano técnico de mobilização (Sprint 08): comparação PWA vs Capacitor vs responsividade pura, recomendação PWA, backlog detalhado.
- Identificados riscos: CORS em produção para SW, suporte a iOS Safari standalone, JWT no localStorage (revisão necessária), fontes Google Fonts offline.

## Pendente
- Decisão do usuário sobre qual opção de mobilização adotar (PWA é a recomendação).
- Definir se o app terá funcionalidade offline (leitura de cache) ou apenas instalabilidade.
- Revisar estratégia de cache do service worker (quais rotas de API cachear, se alguma).
- Alinhar se iOS é alvo prioritário (impacta requisitos do manifest e modo standalone).
- Decisão sobre JWT storage: se migrar para httpOnly cookie no contexto PWA.
- Confirmar se Sprint 08 entra no roadmap oficial (plano.md menciona "app nativo" como fora do escopo v1, mas PWA é web — precisa de validação do produto).

## Próximo passo imediato
Aguardar aprovação do plano pelo usuário. Se aprovado: criar branch docs/s08-planning, registrar decisão em docs/decisoes.md e memory/decisions/, atualizar CLAUDE.md com Sprint 08, e iniciar breakdown detalhado das tarefas para o agente frontend.

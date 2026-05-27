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

> (preenchido ao longo dos sprints)

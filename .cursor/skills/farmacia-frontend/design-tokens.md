# Design Tokens — Farmácia Clark

## Cores CSS (index.css)

```css
--color-bg-deep: #070b14;
--color-bg-elevated: #0f1624;
--color-bg-card: rgba(255, 255, 255, 0.03);

--color-mint: #2dd4a8;
--color-mint-dim: #1a9e7a;
--color-coral: #ff3366;
--color-coral-dim: #cc2952;
--color-amber: #ffb020;
--color-sky: #38bdf8;

--color-text: #f0f4f8;
--color-text-muted: #8b9cb3;
--color-border: rgba(255, 255, 255, 0.08);
```

## NivelControle → badge

| Enum | Label UI | Cor |
|------|----------|-----|
| LIVRE | Livre | mint/10 + texto mint |
| RECEITA_SIMPLES | Receita simples | sky/10 |
| ANTIMICROBIANO | Antimicrobiano | amber/10 |
| CONTROLADO_B1 | Lista B1 | coral/10 |
| CONTROLADO_B2 | Lista B2 | coral/20 |
| CONTROLADO_C1 | Lista C1 | violet |
| CONTROLADO_C2 | Lista C2 | violet escuro |

## Tipografia

- **Outfit** 400/500/600/700 — títulos e corpo
- **JetBrains Mono** 400/500 — EAN-13, códigos ANVISA, IDs

Import Google Fonts no `index.html`.

## Sombras “humanas”

Preferir sombras coloridas leves sobre pretas pesadas:

```css
box-shadow: 0 8px 32px rgba(45, 212, 168, 0.08);
```

Hover em cards interativos: elevar 2px + aumentar opacidade da borda.

## Textura

Overlay de ruído SVG em `body::before` com `opacity: 0.025` — evita flatness digital.

## Layout editorial

- Sidebar fixa 260px; conteúdo com `max-w-7xl` mas hero pode “vazar” 24px à esquerda
- Stat cards com números grandes (Outfit 700) e label pequena uppercase tracking-wide
- Tabelas: zebra sutil `even:bg-white/[0.02]`, hover row `bg-mint/5`

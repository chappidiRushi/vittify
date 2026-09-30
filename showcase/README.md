# Vittify Showcase Website

A minimal, stunning showcase website for [Vittify](https://github.com/chappidiRushi/Vittify) — deployed via GitHub Pages.

**Live site:** [chappidiRushi.github.io/Vittify](https://chappidiRushi.github.io/Vittify/)

## Tech Stack

| | |
|---|---|
| Framework | React 19 |
| Bundler | Vite 7 |
| Styling | Tailwind CSS v4 |
| Animation | Framer Motion |
| Carousel | Embla Carousel |
| Icons | Lucide React |
| Deploy | GitHub Pages via Actions |

## Development

```bash
cd showcase
npm install
npm run dev
# → http://localhost:5173/Vittify/
```

## Production Build

```bash
npm run build
# Output in dist/
```

## Deploy

Push to `main` — GitHub Actions automatically builds and deploys to the `gh-pages` branch.

The workflow is at [`.github/workflows/deploy-showcase.yml`](../.github/workflows/deploy-showcase.yml).

> **GitHub Pages setup:** In your repo settings → Pages → Source, select `Deploy from a branch` → `gh-pages` → `/ (root)`.

## Structure

```
showcase/
├── public/
│   ├── screenshots/     ← All 17 app screenshots
│   ├── banner.png
│   ├── favicon.svg
│   └── .nojekyll
├── src/
│   ├── components/      ← 12 page sections + CTABanner
│   ├── data/            ← Content data files
│   ├── hooks/           ← useInView
│   ├── App.tsx
│   ├── main.tsx
│   └── index.css        ← Design tokens + Tailwind
├── vite.config.ts       ← base: '/Vittify/'
└── package.json
```

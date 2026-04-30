# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

GWSU (Government Web System Unified) is a micro-frontend application built with UmiJS 4 and qiankun. It consists of a main application (master) and multiple sub-applications (slaves) that share a common core library.

## Architecture

```
gwsu-basic-view/
├── apps/
│   ├── gwsu-main/          # Master app (port 8000) - ProLayout shell with qiankun master
│   ├── gwsu-sub-system/    # Sub-app (port 8001) - Main subsystem with dashboard/login
│   └── gwsu-sub-security/  # Sub-app (port 8002) - Security center module
├── gwsu-core/              # Shared library (@gwsu/core) - Theme system, types, utils
└── package.json            # Workspace root with pnpm workspaces
```

### Micro-Frontend Pattern

- **Master App (gwsu-main)**: Uses `@ant-design/pro-components` ProLayout for the shell. Routes to sub-apps via `<MicroApp>` components. Manages global theme state via `ThemeLayout` context.
- **Sub Apps**: Configure `qiankun: { slave: {} }` and set `base` path (`/sub-system`, `/sub-security`). Each has its own `ThemeLayout` wrapper to sync theme from master via `window.postMessage`.

### Theme System

The `@gwsu/core` package provides a multi-theme system:
- 6 themes: ocean (default), forest, violet, amber, graphite, midnight (dark mode)
- `ThemeLayout` component wraps apps, providing `ThemeContext` with `currentTheme` and `changeTheme`
- Theme syncs across micro-apps via `window.postMessage({ type: 'THEME_CHANGE', payload: theme })`
- Applies theme via CSS variables and Ant Design's `ConfigProvider` with `theme.darkAlgorithm`/`defaultAlgorithm`

## Commands

```bash
# Development
pnpm dev:main          # Start master app only (port 8000)
pnpm dev:sub-system    # Start sub-system app only (port 8001)
pnpm dev:sub-security  # Start sub-security app only (port 8002)
pnpm dev:all           # Start all apps in parallel

# Build
pnpm build:core        # Build @gwsu/core first (tsc)
pnpm build:main        # Build master app
pnpm build:sub-system  # Build sub-system app
pnpm build:sub-security # Build sub-security app
pnpm build:all         # Build core then all apps

# Testing (in app directories)
pnpm test              # Run tests with umi test
pnpm test:coverage     # Run tests with coverage

# Clean
pnpm clean             # Remove node_modules, dist, .umi from all packages
```

## Important Patterns

### Adding a New Sub-Application

1. Create app in `apps/` with `qiankun: { slave: {} }` config
2. Add to `pnpm-workspace.yaml`
3. Register in master's `config/config.ts` under `qiankun.master.apps`
4. Add route in master's `config/routes.ts` with `microApp: 'app-name'`
5. Wrap sub-app layout with `ThemeLayout` from `@gwsu/core`

### Modifying Themes

- Theme definitions: `gwsu-core/src/constants/theme.ts`
- Theme types: `gwsu-core/src/types/theme.ts`
- Theme utilities: `gwsu-core/src/utils/theme.ts`
- ThemeLayout component: `gwsu-core/src/components/ThemeLayout.tsx`

### Importing from Core

```tsx
import { ThemeLayout, useThemeContext, themes, getThemeByKey } from '@gwsu/core';
```


### 遵守规则
1. 生成的代码必须是企业级别的，遵循通用规范、逻辑严谨。
2. 禁止使用已经标注过时的方法。
3. 生成界面样式时一定要参考相关技能来生成。
4. 禁止将css样式与代码混合，将css样式 以 modules.less 方式抽离成单独文件。
5. 代码开发时要严格按照组件思想，必要的功能抽历成单独组件。

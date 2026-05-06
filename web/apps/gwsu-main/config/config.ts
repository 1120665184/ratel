import { defineConfig } from 'umi';
import routes from './routes';

export default defineConfig({
  npmClient: 'pnpm',
  mfsu: false,
  plugins: ['@umijs/plugins/dist/qiankun'],
  qiankun: {
    master: {
      apps: [
        { name: 'gwsu-sub-system', entry: '//localhost:8001' },
        { name: 'gwsu-sub-security', entry: '//localhost:8002' },
      ],
    },
  },
  routes,
  // 代理配置
  proxy: {
    '/api': {
      target: 'http://localhost:8888',
      changeOrigin: true,
      pathRewrite: { '^/api': '' },
    },
  },
});

import { defineConfig } from 'umi';
import routes from './routes';

const isDev = process.env.NODE_ENV === 'development';

export default defineConfig({
  npmClient: 'pnpm',
  mfsu: false,
  esbuildMinifyIIFE: true,
  favicons: ['/favicon.jpg'],
  title: 'Ratel Management',
  plugins: ['@umijs/plugins/dist/qiankun'],
  qiankun: {
    master: {
      apps: [
        {
          name: 'gwsu-sub-system',
          entry: isDev ? '//localhost:8001' : '/sub-system/',
        },
        {
          name: 'gwsu-sub-security',
          entry: isDev ? '//localhost:8002' : '/sub-security/',
        },
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

import { defineConfig } from 'umi';
import routes from './routes';

export default defineConfig({
  npmClient: 'pnpm',
  base: '/sub-security',
  publicPath: '/sub-security/',
  plugins: ['@umijs/plugins/dist/qiankun'],
  esbuildMinifyIIFE: true,
  qiankun: {
    slave: {},
  },
  routes,
});
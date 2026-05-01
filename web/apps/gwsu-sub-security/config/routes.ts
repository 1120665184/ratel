export default [
  {
    path: '/',
    component: '@/layouts/index',
    routes: [
      {
        path: '/',
        component: '@/pages/index',
      },
      {
        path: '/menu',
        component: '@/pages/menu',
      },
      {
        path: '/role',
        component: '@/pages/role',
      },
    ],
  },
];

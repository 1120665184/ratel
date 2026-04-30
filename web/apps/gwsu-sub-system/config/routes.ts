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
        path: '/login',
        component: '@/pages/login',
      },
      {
        path: '/dashboard',
        component: '@/pages/dashboard',
      },
      {
        path: '/dept',
        component: '@/pages/dept',
      },
      {
        path: '/dept/org-chart',
        component: '@/pages/dept/org-chart',
      },
      {
        path: '/user',
        component: '@/pages/user',
      },
    ],
  },
];

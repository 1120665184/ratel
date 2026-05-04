export default [
  { path: '/', redirect: '/sub-system/dashboard' },
  {
    path: '/sub-system/*',
    microApp: 'gwsu-sub-system',
  },
  {
    path: '/sub-security/*',
    microApp: 'gwsu-sub-security',
  },
];
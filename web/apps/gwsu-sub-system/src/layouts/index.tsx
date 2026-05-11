import React from 'react';
import { Outlet } from 'umi';
import { ThemeLayout } from '@gwsu/core';

const Layout: React.FC = () => {
  return (
    <ThemeLayout>
      <Outlet />
    </ThemeLayout>
  );
};

export default Layout;

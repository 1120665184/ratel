import {
  DownOutlined,
  SearchOutlined,
} from '@ant-design/icons';
import { Input, Menu, Popover } from 'antd';
import type { MenuProps } from 'antd';
import {
  findMenuByPath,
  findOpenKeys,
  transformToMenuItems,
  useMenuStore,
} from '@gwsu/core';
import type { MenuItem } from '@gwsu/core';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { history, useLocation } from 'umi';
import styles from './index.module.less';

interface RouteSelectorProps {
  /** Tab 是否激活，非激活时隐藏箭头且不可弹出菜单 */
  isActive?: boolean;
}

/**
 * 路由选择器组件
 * 显示当前路由名称，点击展开菜单树供导航
 * 替代传统左侧菜单栏，节省空间
 */
const RouteSelector: React.FC<RouteSelectorProps> = ({ isActive = true }) => {
  const location = useLocation();
  const { menus } = useMenuStore();
  const [open, setOpen] = useState(false);
  const [searchValue, setSearchValue] = useState('');

  // 当前路由名称：从所有菜单中查找，找不到则不显示名称
  const currentMenu = useMemo(
    () => findMenuByPath(menus, location.pathname),
    [menus, location.pathname],
  );
  // 仅精确匹配时显示菜单名称，前缀匹配（非菜单路由）显示 '界面'
  const currentLabel =
    currentMenu?.path === location.pathname ? currentMenu.menuName : '';

  // 搜索过滤菜单
  const filteredMenus = useMemo(() => {
    if (!searchValue.trim()) return menus;
    return filterMenus(menus, searchValue.trim().toLowerCase());
  }, [menus, searchValue]);

  // 转换为 Ant Design Menu items
  const menuItems = useMemo(
    () => (filteredMenus.length > 0 ? transformToMenuItems(filteredMenus) : []),
    [filteredMenus],
  );

  // 选中项所在的目录 keys（用于初始展开和打开时自动展开）
  const selectedOpenKeys = useMemo(
    () => findOpenKeys(menus, location.pathname),
    [menus, location.pathname],
  );

  // 受控展开的目录 keys（手风琴模式：只展开一个）
  const [openKeys, setOpenKeys] = useState<string[]>(selectedOpenKeys);

  // 弹框打开时，路由变化自动同步展开目录
  useEffect(() => {
    if (open) {
      setOpenKeys(selectedOpenKeys);
    }
  }, [selectedOpenKeys, open]);

  // 菜单弹出时，自动展开选中项所在目录
  const handleOpenChange = useCallback((newOpen: boolean) => {
    if (!isActive) return;
    setOpen(newOpen);
    if (newOpen) {
      // 打开时展开选中项所在目录
      setOpenKeys(selectedOpenKeys);
    } else {
      setSearchValue('');
    }
  }, [isActive, selectedOpenKeys]);

  // 目录展开/收起回调 - 手风琴模式：同层级互斥，只允许一个目录展开
  const handleOpenKeysChange: MenuProps['onOpenChange'] = useCallback(
    (keys: string[]) => {
      // 搜索模式下允许全部展开
      if (searchValue) {
        setOpenKeys(keys);
        return;
      }
      // 找到新展开的 key
      const lastKey = keys.find((k) => !openKeys.includes(k));
      if (!lastKey) {
        // 仅收起操作
        setOpenKeys(keys);
        return;
      }
      // 手风琴：关闭同层级其他目录
      const siblingKeys = getSiblingSubmenuKeys(menus, lastKey);
      setOpenKeys(keys.filter((k) => !siblingKeys.includes(k)));
    },
    [menus, openKeys, searchValue],
  );

  // 菜单点击
  const handleMenuClick: MenuProps['onClick'] = useCallback(
    ({ key }:{key:any}) => {
      history.push(key);
      setOpen(false);
      setSearchValue('');
    },
    [],
  );

  // 搜索输入
  const handleSearchChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const value = e.target.value;
      setSearchValue(value);
      if (value) {
        setOpenKeys(getAllKeys(filteredMenus));
      } else {
        setOpenKeys(selectedOpenKeys);
      }
    },
    [filteredMenus, selectedOpenKeys],
  );

  // 弹出内容
  const dropdownContent = (
    <div className={styles.dropdownContent}>
      <div className={styles.searchWrapper}>
        <Input
          prefix={<SearchOutlined />}
          placeholder="搜索菜单..."
          value={searchValue}
          onChange={handleSearchChange}
          variant="borderless"
          allowClear
          className={styles.searchInput}
        />
      </div>
      <div className={styles.menuWrapper}>
        {menuItems.length > 0 ? (
          <Menu
            mode="inline"
            selectedKeys={[location.pathname]}
            openKeys={searchValue ? getAllKeys(filteredMenus) : openKeys}
            onOpenChange={handleOpenKeysChange}
            items={menuItems}
            onClick={handleMenuClick}
            className={styles.menu}
            inlineIndent={16}
          />
        ) : (
          <div className={styles.emptyText}>无匹配结果</div>
        )}
      </div>
    </div>
  );

  // 非激活态：仅显示标签，不渲染 Popover（防止误触弹出菜单）
  if (!isActive) {
    return (
      <div className={styles.selector}>
        <span className={styles.selectorLabel}>{currentLabel || '界面'}</span>
      </div>
    );
  }

  return (
    <Popover
      open={open}
      onOpenChange={handleOpenChange}
      content={dropdownContent}
      trigger="click"
      placement="bottomLeft"
      classNames={
        {
          root: styles.popover
        }
      }
      destroyOnHidden
    >
      <div className={styles.selector}>
        <span className={styles.selectorLabel}>{currentLabel || '界面'}</span>
        <DownOutlined
          className={`${styles.selectorArrow} ${
            open ? styles.selectorArrowOpen : ''
          }`}
        />
      </div>
    </Popover>
  );
};

/**
 * 获取同层级其他目录的 key（手风琴模式：展开一个时关闭同层级其他）
 */
function getSiblingSubmenuKeys(items: MenuItem[], targetKey: string): string[] {
  // 当前层级的所有目录 key
  const submenuKeys = items
    .filter((m) => m.menuType === 1 && m.children?.length)
    .map((m) => m.path);

  // 目标在当前层级，返回同层级其他目录
  if (submenuKeys.includes(targetKey)) {
    return submenuKeys.filter((k) => k !== targetKey);
  }

  // 递归查找子层级
  for (const item of items) {
    if (item.children?.length) {
      const result = getSiblingSubmenuKeys(item.children, targetKey);
      if (result.length > 0) return result;
    }
  }
  return [];
}

/**
 * 搜索过滤菜单树
 */
function filterMenus(menus: MenuItem[], keyword: string): MenuItem[] {
  const result: MenuItem[] = [];
  for (const menu of menus) {
    if (menu.menuType === 3 || !menu.visible || menu.position !== 1) continue;
    const nameMatch = menu.menuName.toLowerCase().includes(keyword);
    const filteredChildren = menu.children ? filterMenus(menu.children, keyword) : [];
    if (nameMatch || filteredChildren.length > 0) {
      result.push({
        ...menu,
        children: nameMatch ? menu.children : filteredChildren,
      });
    }
  }
  return result;
}

/**
 * 获取所有目录的 key，搜索时全部展开
 */
function getAllKeys(menus: MenuItem[]): string[] {
  const keys: string[] = [];
  for (const menu of menus) {
    if (menu.menuType === 1 && menu.children?.length) {
      keys.push(menu.path);
      keys.push(...getAllKeys(menu.children));
    }
  }
  return keys;
}

export default RouteSelector;


import { BgColorsOutlined, CheckOutlined } from '@ant-design/icons';
import { getThemeByKey } from '@gwsu/core';
import { useThemeContext } from '@gwsu/core';
import type { MenuProps } from 'antd';
import { Badge, Dropdown, Tooltip } from 'antd';
import styles from './index.module.less';

export default function () {

  const { currentTheme, changeTheme } = useThemeContext();

  const themes = [
    { key: 'ocean', name: '深海蓝', color: '#1a5fb4' },
    { key: 'forest', name: '森林绿', color: '#2d6a4f' },
    { key: 'violet', name: '紫罗兰', color: '#6b4c9a' },
    { key: 'amber', name: '琥珀橙', color: '#c2510c' },
    { key: 'graphite', name: '石墨灰', color: '#374151' },
    { key: 'midnight', name: '午夜暗色', color: '#1f2937' },
  ];

  const items: MenuProps['items'] = themes.map((theme) => ({
    key: theme.key,
    label: (
      <div className={styles.themeItem}>
        <div
          className={styles.colorPreview}
          style={{
            background: theme.color,
          }}
        >
          {currentTheme.key === theme.key && (
            <CheckOutlined className={styles.checkIcon} />
          )}
        </div>
        <span className={styles.themeName}>{theme.name}</span>
      </div>
    ),
    onClick: () => {
      // 通过 key 获取完整主题配置
      const fullTheme = getThemeByKey(theme.key);
      changeTheme(fullTheme);
    },
  }));

  return (
    <Dropdown
      menu={{ items, selectedKeys: [currentTheme.key] }}
      placement="bottomRight"
      classNames={{ root: styles.dropdown }}
      trigger={['click']}
    >
      <Tooltip title="切换主题">
        <div className={styles.switcher}>
          <Badge
            dot
            color={currentTheme.colors.primary}
            offset={[-2, 2]}
          >
            <BgColorsOutlined className={styles.icon} />
          </Badge>
        </div>
      </Tooltip>
    </Dropdown>
  );
};


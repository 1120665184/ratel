import React from 'react';
// @ts-ignore
import { history } from 'umi';
import { Card, Row, Col, Statistic, Table, Tag, Button } from 'antd';
import {
  UserOutlined,
  RiseOutlined,
  FallOutlined,
  MessageOutlined,
  ThunderboltOutlined,
  SettingOutlined,
  FileTextOutlined,
  BellOutlined,
} from '@ant-design/icons';
import styles from './dashboard.module.less';

interface StatCardProps {
  title: string;
  value: string;
  prefix?: React.ReactNode;
  valueStyle?: React.CSSProperties;
  trend?: 'up' | 'down';
  trendValue?: string;
}

const StatCard: React.FC<StatCardProps> = ({ title, value, prefix, valueStyle, trend, trendValue }) => (
  <Card variant="borderless" className={styles.statCard}>
    <Statistic
      title={title}
      value={value}
      prefix={prefix}
      styles={{ content: valueStyle }}
    />
    {trend && trendValue && (
      <Tag
        className={styles.trendTag}
        color={trend === 'up' ? 'success' : 'error'}
      >
        {trend === 'up' ? <RiseOutlined /> : <FallOutlined />} {trendValue}
      </Tag>
    )}
  </Card>
);

export default function Dashboard() {
  const activityColumns = [
    {
      title: '时间',
      dataIndex: 'time',
      key: 'time',
      width: '80px',
    },
    {
      title: '事件',
      dataIndex: 'event',
      key: 'event',
    },
    {
      title: '用户',
      dataIndex: 'user',
      key: 'user',
      render: (user: string) => (
        <Tag color="blue" icon={<UserOutlined />}>
          {user}
        </Tag>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag
          className={styles.statusTag}
          color={status === '成功' ? 'success' : 'warning'}
        >
          {status}
        </Tag>
      ),
    },
  ];

  const activityData = [
    { key: '1', time: '10:24', event: '新用户注册', user: '张伟', status: '成功' },
    { key: '2', time: '10:18', event: '权限更新', user: '李娜', status: '成功' },
    { key: '3', time: '10:12', event: '数据备份', user: '系统', status: '进行中' },
    { key: '4', time: '09:56', event: '配置变更', user: '王芳', status: '成功' },
    { key: '5', time: '09:42', event: '系统告警', user: '系统', status: '已处理' },
  ];

  const quickActions = [
    { icon: <UserOutlined />, label: '用户管理' },
    { icon: <SettingOutlined />, label: '系统设置' },
    { icon: <FileTextOutlined />, label: '数据报表' },
    { icon: <BellOutlined />, label: '消息中心' },
  ];

  return (
    <div className={styles.dashboard}>
      {/* 页面标题 */}
      <div className={styles.pageHeader}>
        <h2 className={styles.pageTitle}>数据概览</h2>
        <p className={styles.pageSubtitle}>实时监控系统运行状态</p>
      </div>

      {/* 统计卡片 */}
      <Row gutter={[20, 20]} style={{ marginBottom: '24px' }}>
        <Col xs={24} sm={12} lg={6}>
          <StatCard
            title="总用户数"
            value="12,847"
            prefix={<UserOutlined />}
            trend="up"
            trendValue="12.5%"
          />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatCard
            title="活跃用户"
            value="8,421"
            prefix={<ThunderboltOutlined />}
            trend="up"
            trendValue="8.2%"
          />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatCard
            title="系统消息"
            value="1,284"
            prefix={<MessageOutlined />}
            trend="down"
            trendValue="3.1%"
          />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatCard
            title="处理效率"
            value="94.2%"
            prefix={<RiseOutlined />}
            trend="up"
            trendValue="2.4%"
          />
        </Col>
      </Row>

      {/* 内容区域 */}
      <Row gutter={[20, 20]}>
        <Col xs={24} lg={14}>
          <Card title="系统活动" className={styles.contentCard}>
            <Table
              className={styles.activityTable}
              columns={activityColumns}
              dataSource={activityData}
              pagination={false}
              size="small"
            />
          </Card>
        </Col>

        <Col xs={24} lg={10}>
          <Card title="快速操作" className={styles.contentCard}>
            <Row gutter={[16, 16]}>
              {quickActions.map((action, index) => (
                <Col span={12} key={index}>
                  <Button type="default" className={styles.quickActionButton}>
                    <span className={styles.actionIcon}>{action.icon}</span>
                    <span className={styles.actionLabel}>{action.label}</span>
                  </Button>
                </Col>
              ))}
            </Row>
          </Card>
        </Col>
      </Row>
    </div>
  );
}

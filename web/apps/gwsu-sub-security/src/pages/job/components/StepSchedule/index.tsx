import React, { useState } from 'react';
import { Form, Select, Input, InputNumber, Tag, Space, Button } from 'antd';
import { ScheduleOutlined } from '@ant-design/icons';
import { SCHEDULE_TYPE_OPTIONS } from '../../types';
import { getNextTriggerTime } from '../../services/job';

interface StepScheduleProps {
  jobOptions: Array<{ label: string; value: string }>;
}

const StepSchedule: React.FC<StepScheduleProps> = ({ jobOptions }) => {
  const [nextTimes, setNextTimes] = useState<string[]>([]);

  return (
    <>
      <Form.Item name="scheduleType" label="调度类型" rules={[{ required: true, message: '请选择调度类型' }]} initialValue="CRON">
        <Select options={[...SCHEDULE_TYPE_OPTIONS]} placeholder="请选择调度类型" />
      </Form.Item>
      <Form.Item noStyle shouldUpdate>
        {({ getFieldValue }) => {
          const scheduleType = getFieldValue('scheduleType');
          if (scheduleType === 'CRON') {
            return (
              <Form.Item name="scheduleConf" label="Cron表达式" rules={[{ required: true, message: '请输入Cron表达式' }]}>
                <Space.Compact style={{ width: '100%' }}>
                  <Input placeholder="如: 0 0/5 * * * ?" style={{ flex: 1 }} />
                  <Button
                    icon={<ScheduleOutlined />}
                    onClick={async () => {
                      const conf = getFieldValue('scheduleConf');
                      const type = getFieldValue('scheduleType');
                      if (conf && type) {
                        const times = await getNextTriggerTime(type, conf);
                        setNextTimes(times);
                      }
                    }}
                  >
                    预估
                  </Button>
                </Space.Compact>
              </Form.Item>
            );
          }
          if (scheduleType === 'FIX_RATE') {
            return (
              <Form.Item name="scheduleConf" label="间隔(秒)" rules={[{ required: true, message: '请输入间隔秒数' }]}>
                <InputNumber min={1} style={{ width: '100%' }} placeholder="请输入间隔秒数" />
              </Form.Item>
            );
          }
          return null;
        }}
      </Form.Item>
      {nextTimes.length > 0 && (
        <Form.Item label="预估触发时间">
          <Space direction="vertical" size={4}>
            {nextTimes.map((t, i) => (
              <Tag key={i} color="blue">{t}</Tag>
            ))}
          </Space>
        </Form.Item>
      )}
      <Form.Item name="childJobId" label="子任务ID">
        <Select
          mode="multiple"
          placeholder="请选择子任务"
          options={jobOptions}
          showSearch
          optionFilterProp="label"
        />
      </Form.Item>
    </>
  );
};

export default StepSchedule;

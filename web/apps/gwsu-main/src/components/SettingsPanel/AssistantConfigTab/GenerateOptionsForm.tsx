import { useState } from 'react';
import { Form, Slider, InputNumber, Collapse, Input, App } from 'antd';
import type { GenerateOptionsConfig } from './types';
import styles from './GenerateOptionsForm.module.less';

const { TextArea } = Input;

interface GenerateOptionsFormProps {
  value?: GenerateOptionsConfig;
  onChange?: (value: GenerateOptionsConfig) => void;
}

const GenerateOptionsForm: React.FC<GenerateOptionsFormProps> = ({ value, onChange }) => {
  const { message } = App.useApp();
  const [jsonText, setJsonText] = useState<string>(
    () => {
      try {
        return JSON.stringify(value?.additionalBodyParams ?? {}, null, 2);
      } catch {
        return '{}';
      }
    },
  );

  const handleFieldChange = (field: keyof GenerateOptionsConfig, fieldValue: unknown) => {
    onChange?.({ ...value!, [field]: fieldValue });
  };

  const handleJsonChange = (text: string) => {
    setJsonText(text);
    try {
      const parsed = JSON.parse(text);
      onChange?.({ ...value!, additionalBodyParams: parsed });
    } catch {
      // 用户正在编辑中，暂不更新，失焦时校验
    }
  };

  const handleJsonBlur = () => {
    try {
      const parsed = JSON.parse(jsonText);
      if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) {
        message.warning('自定义请求体参数必须为 JSON 对象格式（{}）');
        return;
      }
      const formatted = JSON.stringify(parsed, null, 2);
      setJsonText(formatted);
      onChange?.({ ...value!, additionalBodyParams: parsed });
    } catch {
      message.warning('自定义请求体参数 JSON 格式无效，请检查');
    }
  };

  return (
    <Collapse
      defaultActiveKey={['params']}
      items={[
        {
          key: 'params',
          label: '生成参数',
          children: (
            <>
              <Form.Item label="温度 (Temperature)">
                <Slider
                  min={0}
                  max={2}
                  step={0.1}
                  value={value?.temperature ?? 0.2}
                  onChange={(val) => handleFieldChange('temperature', val)}
                  marks={{ 0: '0', 0.2: '0.2', 1: '1', 2: '2' }}
                />
              </Form.Item>
              <Form.Item label="Top P">
                <Slider
                  min={0}
                  max={1}
                  step={0.05}
                  value={value?.topP}
                  onChange={(val) => handleFieldChange('topP', val)}
                  marks={{ 0: '0', 0.5: '0.5', 1: '1' }}
                />
              </Form.Item>
              <Form.Item label="最大 Token 数">
                <InputNumber
                  min={1}
                  max={128000}
                  value={value?.maxTokens}
                  onChange={(val) => handleFieldChange('maxTokens', val)}
                  placeholder="不限制"
                  style={{ width: '100%' }}
                />
              </Form.Item>
              <Form.Item label="频率惩罚 (Frequency Penalty)">
                <Slider
                  min={-2}
                  max={2}
                  step={0.1}
                  value={value?.frequencyPenalty}
                  onChange={(val) => handleFieldChange('frequencyPenalty', val)}
                  marks={{ '-2': '-2', 0: '0', 2: '2' }}
                />
              </Form.Item>
              <Form.Item label="存在惩罚 (Presence Penalty)">
                <Slider
                  min={-2}
                  max={2}
                  step={0.1}
                  value={value?.presencePenalty}
                  onChange={(val) => handleFieldChange('presencePenalty', val)}
                  marks={{ '-2': '-2', 0: '0', 2: '2' }}
                />
              </Form.Item>
            </>
          ),
        },
        {
          key: 'additionalBodyParams',
          label: '自定义请求体参数',
          children: (
            <>
              <div className={styles.jsonEditorHint}>
                以 JSON 格式配置提供商特有的非标准参数，将合并到请求体中
              </div>
              <Form.Item>
                <TextArea
                  value={jsonText}
                  onChange={(e) => handleJsonChange(e.target.value)}
                  onBlur={handleJsonBlur}
                  placeholder='{"thinking": {"type": "enabled"}}'
                  autoSize={{ minRows: 4, maxRows: 12 }}
                  className={styles.jsonEditor}
                />
              </Form.Item>
            </>
          ),
        },
      ]}
    />
  );
};

export default GenerateOptionsForm;

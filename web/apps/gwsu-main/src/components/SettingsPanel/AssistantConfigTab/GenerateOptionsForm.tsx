import { Form, Slider, InputNumber, Collapse } from 'antd';
import type { GenerateOptionsConfig } from './types';

interface GenerateOptionsFormProps {
  value?: GenerateOptionsConfig;
  onChange?: (value: GenerateOptionsConfig) => void;
}

const GenerateOptionsForm: React.FC<GenerateOptionsFormProps> = ({ value, onChange }) => {
  const handleFieldChange = (field: keyof GenerateOptionsConfig, fieldValue: unknown) => {
    onChange?.({ ...value!, [field]: fieldValue });
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
                  value={value?.temperature ?? 0.7}
                  onChange={(val) => handleFieldChange('temperature', val)}
                  marks={{ 0: '0', 0.7: '0.7', 1: '1', 2: '2' }}
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
      ]}
    />
  );
};

export default GenerateOptionsForm;

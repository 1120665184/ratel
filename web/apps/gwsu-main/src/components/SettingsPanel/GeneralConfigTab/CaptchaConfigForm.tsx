import { Input, InputNumber, Select, Switch } from 'antd';
import {
  ClockCircleOutlined,
  FieldTimeOutlined,
  SafetyCertificateOutlined,
  TagsOutlined,
  FontColorsOutlined,
} from '@ant-design/icons';
import type { CaptchaConfig, CaptchaType, CaptchaTypeOption } from './types';
import styles from './CaptchaConfigForm.module.less';

interface CaptchaConfigFormProps {
  value?: CaptchaConfig;
  typeOptions: CaptchaTypeOption[];
  typeLoading?: boolean;
  onChange?: (value: CaptchaConfig) => void;
}

const CaptchaConfigForm: React.FC<CaptchaConfigFormProps> = ({
  value,
  typeOptions,
  typeLoading = false,
  onChange,
}) => {
  const handleFieldChange = <K extends keyof CaptchaConfig>(field: K, fieldValue: CaptchaConfig[K]) => {
    onChange?.({ ...value!, [field]: fieldValue });
  };

  const selectOptions = typeOptions.map((item) => ({
    label: item.value,
    value: item.key,
  }));

  return (
    <div className={styles.captchaConfigForm}>
      <div className={styles.configItem}>
        <div className={styles.configIcon}>
          <SafetyCertificateOutlined aria-hidden="true" />
        </div>
        <div className={styles.configContent}>
          <span className={styles.configLabel}>启用图形验证码</span>
          <span className={styles.configDesc}>开启后密码登录会要求先完成图形验证码校验</span>
        </div>
        <Switch
          checked={value?.enabled ?? true}
          onChange={(checked) => handleFieldChange('enabled', checked)}
          aria-label="启用图形验证码"
        />
      </div>

      <div className={styles.configItem}>
        <div className={styles.configIcon}>
          <TagsOutlined aria-hidden="true" />
        </div>
        <div className={styles.configContent}>
          <span className={styles.configLabel}>默认验证码类型</span>
          <Select<CaptchaType>
            className={styles.configInput}
            value={value?.type}
            loading={typeLoading}
            options={selectOptions}
            onChange={(selected) => handleFieldChange('type', selected)}
            placeholder="请选择验证码类型"
            aria-label="默认验证码类型"
          />
        </div>
      </div>

      <div className={styles.configItem}>
        <div className={styles.configIcon}>
          <FontColorsOutlined aria-hidden="true" />
        </div>
        <div className={styles.configContent}>
          <span className={styles.configLabel}>水印文字</span>
          <Input
            className={styles.configInput}
            value={value?.waterMark}
            onChange={(e) => handleFieldChange('waterMark', e.target.value)}
            placeholder="Ratel-Manager"
            aria-label="水印文字"
          />
        </div>
      </div>

      <div className={styles.configItem}>
        <div className={styles.configIcon}>
          <ClockCircleOutlined aria-hidden="true" />
        </div>
        <div className={styles.configContent}>
          <span className={styles.configLabel}>验证码有效时间</span>
          <InputNumber
            className={styles.configInput}
            min={30}
            max={3600}
            addonAfter="秒"
            value={value?.expireSeconds}
            onChange={(num) => handleFieldChange('expireSeconds', num ?? 120)}
            aria-label="验证码有效时间"
          />
        </div>
      </div>

      <div className={styles.configItem}>
        <div className={styles.configIcon}>
          <FieldTimeOutlined aria-hidden="true" />
        </div>
        <div className={styles.configContent}>
          <span className={styles.configLabel}>二次校验凭证有效时间</span>
          <InputNumber
            className={styles.configInput}
            min={30}
            max={3600}
            addonAfter="秒"
            value={value?.verificationExpireSeconds}
            onChange={(num) => handleFieldChange('verificationExpireSeconds', num ?? 180)}
            aria-label="二次校验凭证有效时间"
          />
        </div>
      </div>
    </div>
  );
};

export default CaptchaConfigForm;

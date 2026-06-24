import { Input } from 'antd';
import { GlobalOutlined, ApiOutlined } from '@ant-design/icons';
import type { BaseUrlConfig } from './types';
import styles from './ProjectUrlForm.module.less';

interface ProjectUrlFormProps {
  value?: BaseUrlConfig;
  onChange?: (value: BaseUrlConfig) => void;
}

const ProjectUrlForm: React.FC<ProjectUrlFormProps> = ({ value, onChange }) => {
  const handleFieldChange = (field: keyof BaseUrlConfig, fieldValue: string) => {
    onChange?.({ ...value!, [field]: fieldValue });
  };

  return (
    <div className={styles.projectUrlForm}>
      <div className={styles.urlItem}>
        <div className={styles.urlIcon}>
          <GlobalOutlined aria-hidden="true" />
        </div>
        <div className={styles.urlContent}>
          <span className={styles.urlLabel}>前端地址</span>
          <Input
            className={styles.urlInput}
            value={value?.viewBaseUrl}
            onChange={(e) => handleFieldChange('viewBaseUrl', e.target.value)}
            placeholder="http://127.0.0.1:8000"
            aria-label="前端地址"
          />
        </div>
      </div>
      <div className={styles.urlItem}>
        <div className={styles.urlIcon}>
          <ApiOutlined aria-hidden="true" />
        </div>
        <div className={styles.urlContent}>
          <span className={styles.urlLabel}>后端 API 地址</span>
          <Input
            className={styles.urlInput}
            value={value?.apiBaseUrl}
            onChange={(e) => handleFieldChange('apiBaseUrl', e.target.value)}
            placeholder="http://127.0.0.1:8888"
            aria-label="后端 API 地址"
          />
        </div>
      </div>
    </div>
  );
};

export default ProjectUrlForm;

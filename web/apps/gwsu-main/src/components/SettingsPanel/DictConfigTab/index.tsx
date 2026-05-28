import { useState } from 'react';
import DictKeyList from './DictKeyList';
import DictValueList from './DictValueList';
import type { DictInfo } from '../services/dict';
import styles from './index.module.less';

const DictConfigTab: React.FC = () => {
  const [selectedDict, setSelectedDict] = useState<DictInfo | null>(null);

  return (
    <div className={styles.dictConfigTab}>
      <DictKeyList onSelect={setSelectedDict} selectedId={selectedDict?.id} />
      <DictValueList dict={selectedDict} />
    </div>
  );
};

export default DictConfigTab;

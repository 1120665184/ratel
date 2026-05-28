import React, { useState } from 'react';
import DictKeyList from './DictKeyList';
import DictValueList from './DictValueList';
import type { DictInfo } from '../services/dict';
import styles from './index.module.less';

const DictConfigTab: React.FC = () => {
  const [selectedDict, setSelectedDict] = useState<DictInfo | null>(null);

  return (
    <div className={styles.dictConfigTab}>
      <div className={styles.dictKeyPanel}>
        <DictKeyList
          selectedDict={selectedDict}
          onSelect={setSelectedDict}
        />
      </div>
      <div className={styles.dictValuePanel}>
        <DictValueList selectedDict={selectedDict} />
      </div>
    </div>
  );
};

export default DictConfigTab;

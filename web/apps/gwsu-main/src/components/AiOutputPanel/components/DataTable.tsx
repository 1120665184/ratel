import type { BaseComponentProps } from '@json-render/react';
import styles from './DataTable.module.less';

interface ColumnDef {
  key: string;
  label: string;
  width?: string | null;
}

interface DataTableProps {
  title?: string | null;
  columns: ColumnDef[];
  data: Record<string, string | number | boolean | null>[];
  bordered?: boolean | null;
  striped?: boolean | null;
}

const DataTable: React.FC<BaseComponentProps<DataTableProps>> = ({ props }) => {
  const isBordered = props.bordered !== false;
  const isStriped = props.striped !== false;

  return (
    <div className={styles.tableContainer}>
      {props.title && <div className={styles.tableTitle}>{props.title}</div>}
      <table className={styles.table}>
        <thead>
          <tr>
            {props.columns.map((col) => (
              <th
                key={col.key}
                className={`${styles.th} ${isBordered ? styles.borderedTd : ''}`}
                style={col.width ? { width: col.width } : undefined}
              >
                {col.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {props.data.map((row, rowIdx) => (
            <tr
              key={rowIdx}
              className={isStriped && rowIdx % 2 === 1 ? styles.stripedRow : ''}
            >
              {props.columns.map((col) => (
                <td
                  key={col.key}
                  className={`${styles.td} ${isBordered ? styles.borderedTd : ''}`}
                >
                  {String(row[col.key] ?? '')}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default DataTable;

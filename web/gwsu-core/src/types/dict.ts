/**
 * 字典相关类型定义
 */

/** 字典值信息 */
export interface DictValueVO {
  /** 主键ID */
  id: string;
  /** 所属字典键 */
  dictKey: string;
  /** 字典值 */
  dictValue: string;
  /** 字典标签 */
  dictLabel: string;
  /** 排序号 */
  sort: number;
}

/** 字典批量查询结果：字典键 -> 字典值列表 */
export type DictValueMap = Record<string, DictValueVO[]>;

export type { DictInfo, DictValueInfo, DictQuery } from '../services/dict';

/** 字典表单值 */
export interface DictFormValues {
  dictKey: string;
  dictName: string;
  description?: string;
}

/** 字典值表单值 */
export interface DictValueFormValues {
  dictValue: string;
}

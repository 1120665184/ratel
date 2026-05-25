/**
 * 密码加密工具
 * 前端传输密码前进行 MD5 加密，避免明文传输
 */
import md5 from 'js-md5';

/**
 * 对密码进行 MD5 加密
 * @param password 原始密码
 * @returns MD5 加密后的密码（32位小写十六进制字符串）
 */
export function encryptPassword(password: string): string {
  // @ts-ignore
  return md5(password);
}

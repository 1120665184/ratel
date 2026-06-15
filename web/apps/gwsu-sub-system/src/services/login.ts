/**
 * 登录相关 API 服务
 */

import {post} from '@gwsu/core';

/**
 * 登录响应数据
 */
export interface LoginToken {
    /** 用户ID */
    userId: number;
    /** 登录token */
    token: string;
    /** 有效期（秒） */
    expires: number;
    /** 告警消息 */
    alterMsg?: string;
    /** 扩展数据 */
    extraData?: Record<string, string>;
}

/**
 * 终端类型
 */
export enum TerminalType {
    PC = 'PC',
    WEB = 'WEB',
    MOBILE = 'MOBILE',
    APP = 'APP',
    HD = 'HD',
}

/**
 * 登录请求参数
 */
export interface LoginParams {
    /** 登录类型 */
    type: string;
    /** 终端类型 */
    terminal: TerminalType;
    /** 用户名 */
    username: string;
    /** 密码 */
    password: string;
}

/**
 * 无头浏览器快速登录请求参数
 */
export interface HeadlessLoginParams {
    /** 登录类型 */
    type: 'headless';
    /** 终端类型 */
    terminal: TerminalType.PC;
    /** 临时认证凭证 */
    certificationKey: string;
}

/**
 * 用户登录
 * @param params 登录参数
 */
export async function login(params: LoginParams): Promise<LoginToken> {
    const response = await post<LoginToken>(
        `/system/auth/login/manager`,
        params
    );
    return response.data;
}

/**
 * 无头浏览器快速登录
 * @param params 无头登录参数
 */
export async function headlessLogin(params: HeadlessLoginParams): Promise<LoginToken> {
    const response = await post<LoginToken>(
        `/system/auth/login/manager`,
        params
    );
    return response.data;
}

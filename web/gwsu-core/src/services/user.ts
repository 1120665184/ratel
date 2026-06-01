/**
 * 用户服务 API
 */

import { get } from '../utils/request';
import type { UserInfo } from '../stores/userStore';

/** 后端 /manager/current 返回的原始数据 */
interface CurrentUserRaw {
    userId: string;
    userName: string;
    nickname?: string;
    avatar?: string;
    email?: string;
    phone?: string;
    gender?: number;
    status?: number;
    deptId?: string;
    admin?: boolean;
    depts?: Array<{
        id?: string;
        deptId: string;
        deptName: string;
        isPrimary?: boolean;
    }>;
}

/**
 * 获取当前登录用户详细信息
 * 将后端原始数据转换为前端 UserInfo，提取 depts 并设置主部门 deptName
 */
export async function fetchCurrentUserInfo(): Promise<UserInfo> {
    const response = await get<CurrentUserRaw>('/system/manager/current');
    const raw = response.data;

    // 转换 depts 数组
    const depts = raw.depts?.map((d) => ({
        id: d.id,
        deptId: d.deptId,
        deptName: d.deptName,
        isPrimary: d.isPrimary,
    })) ?? [];

    // 主部门名称：优先从 depts 中取 isPrimary=true 的，否则取第一个
    const primaryDept = depts.find((d) => d.isPrimary) ?? depts[0];

    return {
        userId: Number(raw.userId),
        username: raw.userName,
        nickname: raw.nickname,
        avatar: raw.avatar,
        email: raw.email,
        phone: raw.phone,
        gender: raw.gender,
        status: raw.status,
        deptId: raw.deptId ? Number(raw.deptId) : undefined,
        deptName: primaryDept?.deptName,
        depts,
        admin: raw.admin,
    };
}

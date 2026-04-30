import React, {useState, useEffect} from 'react';
// @ts-ignore
import {history} from 'umi';
import {message} from 'antd';
import InteractiveCat from '../components/InteractiveCat';
import {EventType, emitEvent, useMenuStore, useUserStore, fetchCurrentUserInfo} from '@gwsu/core';
import {login, TerminalType} from '../services/login';
import styles from './login.module.less';

export default function Login() {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [loading, setLoading] = useState(false);
    const [isTyping, setIsTyping] = useState(false);
    const [typingTimeout, setTypingTimeout] = useState<NodeJS.Timeout | null>(null);

    // 检测输入状态
    useEffect(() => {
        return () => {
            if (typingTimeout) {
                clearTimeout(typingTimeout);
            }
        };
    }, [typingTimeout]);

    const handleInputChange = (setter: (value: string) => void) => (
        e: React.ChangeEvent<HTMLInputElement>
    ) => {
        setter(e.target.value);
        setIsTyping(true);

        // 清除之前的定时器
        if (typingTimeout) {
            clearTimeout(typingTimeout);
        }

        // 1.5秒后停止输入状态
        const timeout = setTimeout(() => {
            setIsTyping(false);
        }, 1500);
        setTypingTimeout(timeout);
    };

    const handleLogin = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!username.trim() || !password.trim()) {
            message.warning('请输入用户名和密码');
            return;
        }

        setLoading(true);

        try {
            const loginToken = await login({
                type: 'password',
                terminal: TerminalType.WEB,
                username: username.trim(),
                password,
            });

            // 计算 token 过期时间
            const expireTime = Date.now() + loginToken.expires * 1000;

            // 使用 userStore 存储 token 信息
            useUserStore.getState().setTokenInfo({
                token: loginToken.token,
                userId: loginToken.userId,
                expires: loginToken.expires,
                expireTime,
            });

            // 获取用户详细信息
            const userInfo = await fetchCurrentUserInfo();
            useUserStore.getState().setUserInfo(userInfo);

            // 显示告警消息（如有）
            if (loginToken.alterMsg) {
                message.warning(loginToken.alterMsg);
            }

            // 加载菜单数据
            await useMenuStore.getState().loadMenus();

            // 通知主应用登录成功，显示菜单
            emitEvent(EventType.LOGIN_SUCCESS);

            message.success('登录成功');
        } catch (error) {
            // 错误提示已在 request.ts 中统一处理
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className={styles.container}>
            <div className={styles.bgPattern}/>

            {/* 交互式小猫 */}
            <div className={styles.catWrapper}>
                <InteractiveCat isTyping={isTyping} size={180}/>
            </div>

            <div className={styles.card}>
                <div className={styles.logoSection}>
                    <div className={styles.logoIcon}>
                        <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
                            <rect width="48" height="48" rx="12" fill="#1a1a2e"/>
                            <path d="M14 24L24 14L34 24L24 34L14 24Z" stroke="#e94560" strokeWidth="2" fill="none"/>
                            <circle cx="24" cy="24" r="4" fill="#e94560"/>
                        </svg>
                    </div>
                    <h1 className={styles.title}>GWSU</h1>
                    <p className={styles.subtitle}>子系统管理平台</p>
                </div>

                <form onSubmit={handleLogin} className={styles.form}>
                    <div className={styles.inputGroup}>
                        <label className={styles.label}>用户名</label>
                        <input
                            type="text"
                            value={username}
                            onChange={handleInputChange(setUsername)}
                            placeholder="请输入用户名"
                            className={styles.input}
                            required
                        />
                    </div>

                    <div className={styles.inputGroup}>
                        <label className={styles.label}>密码</label>
                        <input
                            type="password"
                            value={password}
                            onChange={handleInputChange(setPassword)}
                            placeholder="请输入密码"
                            className={styles.input}
                            required
                        />
                    </div>

                    <div className={styles.options}>
                        <label className={styles.checkbox}>
                            <input type="checkbox" className={styles.checkboxInput}/>
                            <span className={styles.checkboxCustom}/>
                            <span className={styles.rememberText}>记住我</span>
                        </label>
                        <a href="#" className={styles.forgotLink}>忘记密码？</a>
                    </div>

                    <button
                        type="submit"
                        className={`${styles.button} ${loading ? styles.buttonLoading : ''}`}
                        disabled={loading}
                    >
                        {loading ? (
                            <span className={styles.loadingDots}>
                <span className={styles.dot}>●</span>
                <span className={styles.dot}>●</span>
                <span className={styles.dot}>●</span>
              </span>
                        ) : (
                            '登 录'
                        )}
                    </button>
                </form>

                <div className={styles.footer}>
                    <span className={styles.footerText}>还没有账号？</span>
                    <a href="#" className={styles.signupLink}>立即注册</a>
                </div>
            </div>
        </div>
    );
}

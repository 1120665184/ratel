import React, {useState} from 'react';
// @ts-ignore
import {history} from 'umi';
import {App} from 'antd';
import {EventType, emitEvent, useMenuStore, useUserStore, fetchCurrentUserInfo, encryptPassword} from '@gwsu/core';
import {login, TerminalType} from '../services/login';
import styles from './login.module.less';

export default function Login() {
    const {message} = App.useApp();
    const [username, setUsername] = useState('admin');
    const [password, setPassword] = useState('admin123');
    const [loading, setLoading] = useState(false);

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
                password: encryptPassword(password),
            });

            const expireTime = Date.now() + loginToken.expires * 1000;

            useUserStore.getState().setTokenInfo({
                token: loginToken.token,
                userId: loginToken.userId,
                expires: loginToken.expires,
                expireTime,
            });

            const userInfo = await fetchCurrentUserInfo();
            useUserStore.getState().setUserInfo(userInfo);

            if (loginToken.alterMsg) {
                message.warning(loginToken.alterMsg);
            }

            await useMenuStore.getState().loadMenus();

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
            {/* 左侧展示区 */}
            <div className={styles.showcase}>
                <div className={styles.brandName}>Ratel</div>

                <div className={styles.quoteSection}>
                    <div className={styles.quoteBar}/>
                    <div className={styles.quoteContent}>
                        <div className={styles.chineseText}>
                            {'载营魄抱一'.split('').map((char, i) => (
                                <span key={i} className={styles.bounceChar} style={{animationDelay: `${i * 0.15}s`}}>{char}</span>
                            ))}
                        </div>
                        <div className={styles.chineseText}>
                            {'能无离乎'.split('').map((char, i) => (
                                <span key={i} className={styles.bounceChar} style={{animationDelay: `${(i + 5) * 0.15}s`}}>{char}</span>
                            ))}
                        </div>
                        <div className={styles.englishText}>
                            Adhere to the <span className={styles.highlightLetter}>G</span>reat{' '}
                            <span className={styles.highlightLetter}>W</span>ay, remain{' '}
                            <span className={styles.highlightLetter}>S</span>ingle-minded and{' '}
                            <span className={styles.highlightLetter}>U</span>navering
                        </div>
                    </div>
                </div>

                {/* 装饰元素 */}
                <div className={styles.decoCircle}/>
                <div className={styles.decoDot1}/>
                <div className={styles.decoDot2}/>
            </div>

            {/* 右侧登录区 */}
            <div className={styles.loginSection}>
                <div className={styles.logoIcon}>
                    <div className={styles.diamond}/>
                </div>
                <div className={styles.loginTitle}>登 录</div>

                <form onSubmit={handleLogin} className={styles.form}>
                    <div className={styles.inputGroup}>
                        <label className={styles.label}>用户名</label>
                        <input
                            type="text"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            placeholder="请输入用户名"
                            className={styles.input}
                            autoComplete="username"
                            required
                        />
                    </div>

                    <div className={styles.inputGroup}>
                        <label className={styles.label}>密码</label>
                        <input
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            placeholder="请输入密码"
                            className={styles.input}
                            autoComplete="current-password"
                            required
                        />
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


            </div>
        </div>
    );
}

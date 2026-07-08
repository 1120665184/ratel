package org.quyq.gwsu.common.job.constant;

/**
 * 任务常量
 */
public class JobConst {

    // ---------------------- for executor ----------------------

    /**
     * 优雅关闭等待秒数
     */
    public static final long ELEGANT_SHUTDOWN_WAITING_SECONDS = 5;

    /**
     * 执行成功
     */
    public static final int HANDLE_CODE_SUCCESS = 200;

    /**
     * 执行失败
     */
    public static final int HANDLE_CODE_FAIL = 500;

    /**
     * 执行超时
     */
    public static final int HANDLE_CODE_TIMEOUT = 502;

    // ---------------------- for registry ----------------------

    /**
     * 注册心跳间隔，默认30秒
     */
    public static final int REGISTRY_BEAT_INTERVAL = 30;

    /**
     * GLUE任务公共执行器注册键
     */
    public static final String GLUE_REGISTRY_KEY = "__glue__";

}

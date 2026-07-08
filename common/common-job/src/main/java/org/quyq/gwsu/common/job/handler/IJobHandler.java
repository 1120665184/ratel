package org.quyq.gwsu.common.job.handler;

/**
 * 任务处理器基类
 */
public abstract class IJobHandler {

    /**
     * 执行任务，当执行器收到调度请求时调用
     */
    public abstract void execute() throws Exception;

    /**
     * 初始化，当JobThread初始化时调用
     */
    public void init() throws Exception {
        // do something
    }

    /**
     * 销毁，当JobThread销毁时调用
     */
    public void destroy() throws Exception {
        // do something
    }

}

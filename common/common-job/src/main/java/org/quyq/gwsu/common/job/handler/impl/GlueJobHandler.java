package org.quyq.gwsu.common.job.handler.impl;

import org.quyq.gwsu.common.job.context.XxlJobHelper;
import org.quyq.gwsu.common.job.handler.IJobHandler;

/**
 * Glue任务处理器
 */
public class GlueJobHandler extends IJobHandler {

    /**
     * glue更新时间
     */
    private long glueUpdatetime;

    /**
     * 任务处理器
     */
    private IJobHandler jobHandler;

    public GlueJobHandler(IJobHandler jobHandler, long glueUpdatetime) {
        this.jobHandler = jobHandler;
        this.glueUpdatetime = glueUpdatetime;
    }

    public long getGlueUpdatetime() {
        return glueUpdatetime;
    }

    @Override
    public void execute() throws Exception {
        XxlJobHelper.log("----------- glue.version:" + glueUpdatetime + " -----------");
        this.jobHandler.execute();
    }

    @Override
    public void init() throws Exception {
        this.jobHandler.init();
    }

    @Override
    public void destroy() throws Exception {
        this.jobHandler.destroy();
    }
}

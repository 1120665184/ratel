package org.quyq.gwsu.common.log.dto;


/**
 * @author Quyq
 * @date 2026/5/14
 * @description 日志存储生命周期
 */

public record LogLifeCycle(
        /**
         * 指定天数后进入“冷”阶段
         * 存储源是ES时，该阶段会讲数据冻结 ， 此时数据为不可查询状态
         * 需要将数据解冻，才能查询,ES相关接口：POST {索引名}/_unfreeze
         */
        Integer coldMinAge ,
        /**
         * 指定天数后会进行删除
         */
        Integer deleteMinAge
) {
}

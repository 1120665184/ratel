package org.quyq.gwsu.common.security.utils;


import org.quyq.gwsu.common.api.utils.FeignUtils;
import org.quyq.gwsu.common.core.utils.SpringUtils;
import org.quyq.gwsu.common.security.api.IDictInfoClientApi;
import org.quyq.gwsu.common.security.api.vo.DictValueVO;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Quyq
 * @date 2026/5/30
 * @description 字典信息获取工具类
 */
public class DictInfoUtils {

    private DictInfoUtils() {
    }

    /**
     * 获取指定字典数据
     * @param dictKey
     * @return
     */
    public static Map<String , String> get(String dictKey){
        return get(Collections.singletonList(dictKey))
                .get(dictKey);
    }

    public static Map<String , Map<String ,String>> get(List<String> dictKeys){
        if(CollectionUtils.isEmpty(dictKeys)){
            return Map.of();
        }

        Map<String, List<DictValueVO>> dictValues = getDictValues(dictKeys);
        Map<String , Map<String ,String>> finV = new LinkedHashMap<>(dictValues.size());
        dictValues.forEach((k,v)->
                        finV.put(k ,v.stream().collect(Collectors
                                .toMap(DictValueVO::getDictValue, DictValueVO::getDictLabel ,
                                        (v1 , v2) -> v2 , LinkedHashMap::new)))
                );

        return finV;

    }

    private static Map<String, List<DictValueVO>> getDictValues(List<String> dictKeys) {
        return FeignUtils.data(SpringUtils.getBean(IDictInfoClientApi.class)
                .getDictValueByDictKeyBatch(dictKeys));
    }

}

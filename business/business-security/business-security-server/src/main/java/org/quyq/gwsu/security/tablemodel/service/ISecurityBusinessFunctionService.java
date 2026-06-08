package org.quyq.gwsu.security.tablemodel.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.quyq.gwsu.security.api.tablemodel.dto.BusinessFunctionQueryDTO;
import org.quyq.gwsu.security.api.tablemodel.vo.BusinessFunctionDetailVO;
import org.quyq.gwsu.security.api.tablemodel.vo.BusinessFunctionVO;
import org.quyq.gwsu.security.tablemodel.domain.SecurityBusinessFunction;

import java.util.List;

public interface ISecurityBusinessFunctionService extends IService<SecurityBusinessFunction> {

    BusinessFunctionVO getById(String id);

    BusinessFunctionDetailVO getDetailById(String id);

    List<BusinessFunctionVO> listAll();

    IPage<BusinessFunctionVO> pageByCondition(BusinessFunctionQueryDTO query);

    Boolean saveOrUpdateFunction(BusinessFunctionVO vo);

    Boolean removeByIds(List<String> ids);
}

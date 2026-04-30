package org.quyq.gwsu.system.dept.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.quyq.gwsu.system.api.dept.dto.DeptSaveDTO;
import org.quyq.gwsu.system.api.dept.vo.DeptTreeVO;
import org.quyq.gwsu.system.api.dept.vo.DeptVO;
import org.quyq.gwsu.system.api.dept.vo.UserDeptDetailVO;
import org.quyq.gwsu.system.dept.domain.SysDept;

import java.util.List;
import java.util.Map;

/**
 * 部门服务接口
 *
 * @author Quyq
 */
public interface ISysDeptService extends IService<SysDept> {

    /**
     * 保存部门（新增或更新）
     *
     * @param dto 保存请求
     * @return 部门ID
     */
    String saveDept(DeptSaveDTO dto);

    /**
     * 删除部门
     *
     * @param id 部门ID
     */
    void removeDept(String id);

    /**
     * 获取部门详情
     *
     * @param id 部门ID
     * @return 部门详情
     */
    DeptVO getDeptDetail(String id);

    /**
     * 获取部门树
     *
     * @return 部门树
     */
    List<DeptTreeVO> getDeptTree();

    /**
     * 获取子部门列表
     *
     * @param parentId 父部门ID
     * @return 子部门列表
     */
    List<DeptVO> listChildren(String parentId);

    /**
     * 添加额外父部门
     *
     * @param id 部门ID
     * @param parentId 父部门ID
     */
    void addParent(String id, String parentId);

    /**
     * 移除额外父部门
     *
     * @param id 部门ID
     * @param parentId 父部门ID
     */
    void removeParent(String id, String parentId);

    /**
     * 获取部门下的用户列表
     *
     * @param deptId 部门ID
     * @return 用户部门详情列表
     */
    List<UserDeptDetailVO> listUsersByDept(String deptId);

    /**
     * 获取各部门用户数量
     *
     * @return 部门ID → 用户数量
     */
    Map<String, Long> countUsersByDept();
}
package org.quyq.gwsu.security.tablemodel.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.security.annotation.TableModelPermission;
import org.quyq.gwsu.security.tablemodel.domain.SecurityTableModelColumn;
import org.quyq.gwsu.security.tablemodel.domain.SecurityTableModelForeignKey;
import org.quyq.gwsu.security.tablemodel.domain.SecurityTableModelTable;
import org.quyq.gwsu.security.tablemodel.service.ISecurityTableModelColumnService;
import org.quyq.gwsu.security.tablemodel.service.ISecurityTableModelForeignKeyService;
import org.quyq.gwsu.security.tablemodel.service.ISecurityTableModelTableService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 表模型管理控制器
 *
 * @author Quyq
 */
@Tag(name = "表模型管理")
@RestController
@RequestMapping("tablemodel")
@TableModelPermission({SecurityTableModelTable.class, SecurityTableModelColumn.class, SecurityTableModelForeignKey.class})
@RequiredArgsConstructor
public class SecurityTableModelTableController {

    private final ISecurityTableModelTableService tableModelTableService;

    private final ISecurityTableModelColumnService tableModelColumnService;

    private final ISecurityTableModelForeignKeyService tableModelForeignKeyService;

}

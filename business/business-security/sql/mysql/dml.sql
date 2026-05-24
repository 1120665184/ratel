-- =============================================
-- 菜单管理初始化数据
-- 数据库：MySQL
-- =============================================

-- 初始化角色数据
INSERT INTO security_role (id, role_name, role_code, sort, description, status) VALUES
(1, '超级管理员', 'SUPER_ADMIN', 1, '拥有所有权限', 1),
(2, '普通用户', 'USER', 2, '普通用户角色', 1);

-- 初始化菜单数据
-- 一级目录
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status) VALUES
(1, NULL, '系统管理', 1, 1, 'setting', '/sub-system', 1, 1),
(2, NULL, '安全中心', 1, 2, 'security', '/sub-security', 1, 1);

-- 二级菜单
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, path, micro_app, visible, status) VALUES
(3, 1, '仪表盘', 2, 1, '/sub-system/dashboard', 'gwsu-sub-system', 1, 1),
(4, 1, '用户管理', 2, 2, '/sub-system/user', 'gwsu-sub-system', 1, 1),
(5, 2, '角色管理', 2, 1, '/sub-security/role', 'gwsu-sub-security', 1, 1),
(6, 2, '菜单管理', 2, 2, '/sub-security/menu', 'gwsu-sub-security', 1, 1);

-- 二级菜单
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, path, micro_app, visible, status) VALUES
(3, 1, '仪表盘', 2, 1, '/sub-system/dashboard', 'gwsu-sub-system', 1, 1),
(4, 1, '用户管理', 2, 2, '/sub-system/user', 'gwsu-sub-system', 1, 1),
(22, 1, '部门管理', 2, 3, '/sub-system/dept', 'gwsu-sub-system', 1, 1),
(5, 2, '角色管理', 2, 1, '/sub-security/role', 'gwsu-sub-security', 1, 1),
(6, 2, '菜单管理', 2, 2, '/sub-security/menu', 'gwsu-sub-security', 1, 1);

-- 三级按钮
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, permission, visible, status) VALUES
(7, 4, '新增用户', 3, 1, 'system:user:add', 1, 1),
(8, 4, '编辑用户', 3, 2, 'system:user:edit', 1, 1),
(9, 4, '删除用户', 3, 3, 'system:user:delete', 1, 1),
(23, 22, '新增部门', 3, 1, 'system:dept:add', 1, 1),
(24, 22, '编辑部门', 3, 2, 'system:dept:edit', 1, 1),
(25, 22, '删除部门', 3, 3, 'system:dept:delete', 1, 1),
(26, 22, '分配部门', 3, 4, 'system:dept:assign', 1, 1),
(10, 5, '新增角色', 3, 1, 'security:role:add', 1, 1),
(11, 5, '编辑角色', 3, 2, 'security:role:edit', 1, 1),
(12, 5, '删除角色', 3, 3, 'security:role:delete', 1, 1),
(13, 6, '新增菜单', 3, 1, 'security:menu:add', 1, 1),
(14, 6, '编辑菜单', 3, 2, 'security:menu:edit', 1, 1),
(15, 6, '删除菜单', 3, 3, 'security:menu:delete', 1, 1);

-- 初始化角色菜单关联（超级管理员拥有所有菜单权限）
INSERT INTO security_role_menu (role_id, menu_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6),
(1, 7), (1, 8), (1, 9), (1, 10), (1, 11), (1, 12), (1, 13), (1, 14), (1, 15),
(1, 22), (1, 23), (1, 24), (1, 25), (1, 26);

-- =============================================
-- Catalog管理初始化数据
-- =============================================

-- =============================================
-- 组件池：图表类组件
-- =============================================
INSERT INTO security_catalog_component (id, component_name, description, props_schema, default_props, category, sort_order, status) VALUES
('CT_COMP_001', 'BarChart',
'柱状图组件，用于展示分类数据的对比关系，如各类安全事件数量、各部门权限统计等。支持横向和纵向柱状图。',
'{"type":"object","required":["data"],"properties":{"title":{"type":"string","description":"图表标题"},"data":{"type":"array","items":{"type":"object","required":["label","value"],"properties":{"label":{"type":"string","description":"分类标签"},"value":{"type":"number","description":"数值"},"color":{"type":"string","description":"柱子颜色"}}},"description":"数据项列表"},"direction":{"type":"string","enum":["vertical","horizontal"],"description":"柱状图方向，默认vertical"},"showLegend":{"type":"boolean","description":"是否显示图例，默认true"},"showValue":{"type":"boolean","description":"是否在柱子上显示数值，默认false"}}}',
'{"title":"","direction":"vertical","showLegend":true,"showValue":false}',
'chart', 1, 1),

('CT_COMP_002', 'LineChart',
'折线图组件，用于展示数据随时间变化的趋势，如安全事件趋势、登录频次变化等。支持多折线对比。',
'{"type":"object","required":["data"],"properties":{"title":{"type":"string","description":"图表标题"},"data":{"type":"array","items":{"type":"object","required":["label","values"],"properties":{"label":{"type":"string","description":"X轴标签（通常为时间点）"},"values":{"type":"array","items":{"type":"number"},"description":"各折线对应的Y值"}}},"description":"数据点列表"},"seriesNames":{"type":"array","items":{"type":"string"},"description":"各折线系列名称"},"showArea":{"type":"boolean","description":"是否显示面积填充，默认false"},"showLegend":{"type":"boolean","description":"是否显示图例，默认true"},"smooth":{"type":"boolean","description":"是否平滑曲线，默认true"}}}',
'{"title":"","showArea":false,"showLegend":true,"smooth":true}',
'chart', 2, 1),

('CT_COMP_003', 'PieChart',
'饼图组件，用于展示数据的占比分布，如角色权限分布、事件类型占比等。支持环形图模式。',
'{"type":"object","required":["data"],"properties":{"title":{"type":"string","description":"图表标题"},"data":{"type":"array","items":{"type":"object","required":["label","value"],"properties":{"label":{"type":"string","description":"分类标签"},"value":{"type":"number","description":"数值"},"color":{"type":"string","description":"扇区颜色"}}},"description":"数据项列表"},"ring":{"type":"boolean","description":"是否环形图，默认false"},"showLabel":{"type":"boolean","description":"是否显示标签，默认true"},"showLegend":{"type":"boolean","description":"是否显示图例，默认true"}}}',
'{"title":"","ring":false,"showLabel":true,"showLegend":true}',
'chart', 3, 1),

('CT_COMP_004', 'StatisticCard',
'统计数值卡片组件，用于展示关键指标数值，如今日登录次数、在线用户数、安全事件总数等。支持趋势标识和环比变化。',
'{"type":"object","required":["title","value"],"properties":{"title":{"type":"string","description":"指标标题"},"value":{"type":"number","description":"指标数值"},"precision":{"type":"integer","description":"小数精度，默认0"},"prefix":{"type":"string","description":"数值前缀，如¥"},"suffix":{"type":"string","description":"数值后缀，如%"},"trend":{"type":"string","enum":["up","down","flat"],"description":"趋势方向"},"trendValue":{"type":"number","description":"趋势变化值"},"icon":{"type":"string","description":"图标名称"},"description":{"type":"string","description":"补充说明"}}}',
'{"precision":0}',
'chart', 4, 1);

-- =============================================
-- 组件池：列表/表格类组件
-- =============================================
INSERT INTO security_catalog_component (id, component_name, description, props_schema, default_props, category, sort_order, status) VALUES
('CT_COMP_010', 'DataTable',
'数据表格组件，用于展示结构化的列表数据，如用户列表、角色列表、操作日志等。支持列定义、分页和行操作。',
'{"type":"object","required":["columns","dataSource"],"properties":{"title":{"type":"string","description":"表格标题"},"columns":{"type":"array","items":{"type":"object","required":["key","title"],"properties":{"key":{"type":"string","description":"字段key"},"title":{"type":"string","description":"列标题"},"width":{"type":"number","description":"列宽度"},"align":{"type":"string","enum":["left","center","right"],"description":"对齐方式"},"render":{"type":"string","enum":["text","tag","link","status"],"description":"渲染类型，默认text"}}},"description":"列定义"},"dataSource":{"type":"array","items":{"type":"object"},"description":"数据行列表"},"showIndex":{"type":"boolean","description":"是否显示序号列，默认false"},"bordered":{"type":"boolean","description":"是否显示边框，默认true"},"size":{"type":"string","enum":["small","middle","large"],"description":"表格尺寸，默认middle"},"total":{"type":"integer","description":"数据总量（用于分页）"},"pageSize":{"type":"integer","description":"每页条数，默认10"}}}',
'{"showIndex":false,"bordered":true,"size":"middle","pageSize":10}',
'display', 10, 1),

('CT_COMP_011', 'DescriptionList',
'描述列表组件，用于展示键值对形式的详情信息，如用户详情、角色信息、操作日志详情等。',
'{"type":"object","required":["items"],"properties":{"title":{"type":"string","description":"列表标题"},"items":{"type":"array","items":{"type":"object","required":["label","value"],"properties":{"label":{"type":"string","description":"字段标签"},"value":{"type":"string","description":"字段值"},"span":{"type":"integer","description":"占据列数，默认1"}}},"description":"描述项列表"},"columns":{"type":"integer","description":"每行列数，默认3"},"bordered":{"type":"boolean","description":"是否带边框，默认true"},"size":{"type":"string","enum":["small","default","large"],"description":"尺寸，默认default"}}}',
'{"columns":3,"bordered":true,"size":"default"}',
'display', 11, 1),

('CT_COMP_012', 'TagList',
'标签列表组件，用于展示一组标签信息，如角色标签、权限标签、事件类型标签等。支持不同颜色和状态。',
'{"type":"object","required":["tags"],"properties":{"tags":{"type":"array","items":{"type":"object","required":["label"],"properties":{"label":{"type":"string","description":"标签文本"},"color":{"type":"string","description":"标签颜色，如blue/green/red/orange等"},"icon":{"type":"string","description":"标签图标"}}},"description":"标签列表"},"title":{"type":"string","description":"标题"},"direction":{"type":"string","enum":["horizontal","vertical"],"description":"排列方向，默认horizontal"}}}',
'{"direction":"horizontal"}',
'display', 12, 1);

-- =============================================
-- 组件池：布局与基础组件（A2UI Basic Catalog）
-- =============================================
INSERT INTO security_catalog_component (id, component_name, description, props_schema, default_props, category, sort_order, status) VALUES
('CT_COMP_020', 'Text',
'文本组件，用于展示标题、正文、说明等文字内容。支持h1~h5标题和正文、注释等变体。',
'{"type":"object","required":["text"],"properties":{"text":{"type":"string","description":"文本内容"},"variant":{"type":"string","enum":["h1","h2","h3","h4","h5","body","caption"],"description":"文本变体，默认body"}}}',
'{"variant":"body"}',
'display', 20, 1),

('CT_COMP_021', 'Card',
'卡片容器组件，用于包裹其他组件形成独立的视觉单元，带有阴影和圆角。',
'{"type":"object","properties":{"title":{"type":"string","description":"卡片标题"},"bordered":{"type":"boolean","description":"是否显示边框，默认true"},"hoverable":{"type":"boolean","description":"是否悬停效果，默认false"}}}',
'{"bordered":true,"hoverable":false}',
'display', 21, 1),

('CT_COMP_022', 'Row',
'行布局组件，水平排列子组件。用于构建多列布局。',
'{"type":"object","properties":{"gap":{"type":"number","description":"子元素间距，默认16"},"justify":{"type":"string","enum":["start","center","end","space-between","space-around"],"description":"主轴对齐方式"},"align":{"type":"string","enum":["start","center","end","stretch"],"description":"交叉轴对齐方式"}}}',
'{"gap":16}',
'display', 22, 1),

('CT_COMP_023', 'Column',
'列布局组件，垂直排列子组件。用于构建垂直流式布局。',
'{"type":"object","properties":{"gap":{"type":"number","description":"子元素间距，默认8"},"justify":{"type":"string","enum":["start","center","end","space-between","space-around"],"description":"主轴对齐方式"},"align":{"type":"string","enum":["start","center","end","stretch"],"description":"交叉轴对齐方式"}}}',
'{"gap":8}',
'display', 23, 1),

('CT_COMP_024', 'Tabs',
'选项卡组件，用于在同一区域切换展示不同内容面板，如切换不同维度统计数据。',
'{"type":"object","required":["items"],"properties":{"items":{"type":"array","items":{"type":"object","required":["key","label"],"properties":{"key":{"type":"string","description":"选项卡key"},"label":{"type":"string","description":"选项卡标题"}}},"description":"选项卡列表"},"defaultActiveKey":{"type":"string","description":"默认激活的tab key"}}}',
'{}',
'display', 24, 1),

('CT_COMP_025', 'Divider',
'分割线组件，用于区域间的视觉分隔。',
'{"type":"object","properties":{"title":{"type":"string","description":"分割线标题"},"orientation":{"type":"string","enum":["left","center","right"],"description":"标题位置，默认center"},"dashed":{"type":"boolean","description":"是否虚线，默认false"}}}',
'{"orientation":"center","dashed":false}',
'display', 25, 1);

-- =============================================
-- Catalog定义：默认安全中心输出Catalog
-- =============================================
INSERT INTO security_catalog (id, catalog_key, catalog_name, description, version, active, status) VALUES
('CT_CATALOG_001', 'security-output-default', '安全中心默认输出', '安全中心AI助手的默认输出Catalog，包含图表、列表和基础布局组件，支持统计报表、数据列表和详情展示等场景。', '1.0.0', 1, 1);

-- =============================================
-- Catalog与组件关联：默认Catalog包含所有组件
-- =============================================
INSERT INTO security_catalog_component_ref (id, catalog_id, component_id, sort_order) VALUES
('CT_REF_001', 'CT_CATALOG_001', 'CT_COMP_020', 1),
('CT_REF_002', 'CT_CATALOG_001', 'CT_COMP_021', 2),
('CT_REF_003', 'CT_CATALOG_001', 'CT_COMP_022', 3),
('CT_REF_004', 'CT_CATALOG_001', 'CT_COMP_023', 4),
('CT_REF_005', 'CT_CATALOG_001', 'CT_COMP_024', 5),
('CT_REF_006', 'CT_CATALOG_001', 'CT_COMP_025', 6),
('CT_REF_007', 'CT_CATALOG_001', 'CT_COMP_004', 7),
('CT_REF_008', 'CT_CATALOG_001', 'CT_COMP_001', 8),
('CT_REF_009', 'CT_CATALOG_001', 'CT_COMP_002', 9),
('CT_REF_010', 'CT_CATALOG_001', 'CT_COMP_003', 10),
('CT_REF_011', 'CT_CATALOG_001', 'CT_COMP_010', 11),
('CT_REF_012', 'CT_CATALOG_001', 'CT_COMP_011', 12),
('CT_REF_013', 'CT_CATALOG_001', 'CT_COMP_012', 13);

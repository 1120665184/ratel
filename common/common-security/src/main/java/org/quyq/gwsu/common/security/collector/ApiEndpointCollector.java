package org.quyq.gwsu.common.security.collector;

import cn.hutool.crypto.digest.MD5;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.core.domain.BusinessModuleInfo;
import org.quyq.gwsu.common.core.provider.BusinessModuleInfoProvider;
import org.quyq.gwsu.common.core.utils.DeployUtils;
import org.quyq.gwsu.common.core.utils.ProjectUtils;
import org.quyq.gwsu.common.core.utils.SpringUtils;
import org.quyq.gwsu.common.security.annotation.LoginAllowAccess;
import org.quyq.gwsu.common.security.annotation.SensitiveStrategy;
import org.quyq.gwsu.common.security.annotation.TableModelField;
import org.quyq.gwsu.common.security.annotation.TableModelPermission;
import org.quyq.gwsu.common.security.domain.ApiEndpointInfo;
import org.quyq.gwsu.common.security.domain.FieldPermission;
import org.quyq.gwsu.common.security.domain.TableModelInfo;
import com.baomidou.mybatisplus.annotation.TableName;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.PathPatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

/**
 * HTTP 接口信息收集器
 * 应用启动时收集所有 HTTP 接口信息
 *
 * @author Quyq
 */
@Slf4j
@RequiredArgsConstructor
public class ApiEndpointCollector implements ApplicationRunner {


    private final RequestMappingHandlerMapping handlerMapping;

    private final CacheUtils cacheUtils;

    private final ProjectUtils projectUtils;

    public static final String PERMISSION_API_CHANNEL = "permission:api";


    @Override
    public void run(ApplicationArguments args) {
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = handlerMapping.getHandlerMethods();

        String applicationName = projectUtils.getApplicationName();

        // 获取所有模块信息提供者
        Map<String, BusinessModuleInfo> moduleInfoMap = buildModuleInfoMap();

        List<ApiEndpointInfo> endpoints = new ArrayList<>();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            RequestMappingInfo requestMappingInfo = entry.getKey();
            HandlerMethod handlerMethod = entry.getValue();

            // 跳过框架内置接口
            if (isInternalEndpoint(handlerMethod)) {
                continue;
            }

            ApiEndpointInfo endpointInfo = buildEndpointInfo(requestMappingInfo, handlerMethod, moduleInfoMap);
            if (endpointInfo != null && StringUtils.hasText(endpointInfo.modulePrefix())) {
                endpoints.add(endpointInfo);
            }
        }

        Map<String, List<ApiEndpointInfo>> maps = endpoints.stream().collect(Collectors.groupingBy(ApiEndpointInfo::modulePrefix));
        if (maps.size() != moduleInfoMap.size()) {
            moduleInfoMap.forEach((moduleName, apiEndpointInfo) -> {
                if (!maps.containsKey(moduleName)) {
                    maps.put(moduleName, new ArrayList<>());
                }
            });
        }

        //将api资源推送
        cacheUtils.withRebel(() -> cacheUtils.lPush(PERMISSION_API_CHANNEL, new ApiEndpointWrapper(applicationName, maps)));

    }

    /**
     * 构建模块信息映射表
     */
    private Map<String, BusinessModuleInfo> buildModuleInfoMap() {
        Map<String, BusinessModuleInfo> moduleInfoMap = new HashMap<>();
        List<BusinessModuleInfoProvider> providers = SpringUtils.getBeansOfType(BusinessModuleInfoProvider.class);

        for (BusinessModuleInfoProvider provider : providers) {
            BusinessModuleInfo info = provider.module();
            moduleInfoMap.put(info.prefix(), info);
        }
        return moduleInfoMap;
    }

    /**
     * 判断是否为框架内置接口
     */
    private boolean isInternalEndpoint(HandlerMethod handlerMethod) {
        String packageName = handlerMethod.getBeanType().getPackageName();
        return !packageName.startsWith(CoreConstants.Yaml.PROJECT_CONFIG_PREFIX);
    }

    /**
     * 构建接口信息
     */
    private ApiEndpointInfo buildEndpointInfo(RequestMappingInfo requestMappingInfo,
                                              HandlerMethod handlerMethod,
                                              Map<String, BusinessModuleInfo> moduleInfoMap) {
        // 获取路径
        Set<PathPattern> patterns = Optional.ofNullable(requestMappingInfo.getPathPatternsCondition())
                .map(PathPatternsRequestCondition::getPatterns)
                .orElseGet(Collections::emptySet);
        if (patterns.isEmpty()) {
            return null;
        }
        String originalPath = patterns.iterator().next().getPatternString();

        // 获取请求方法
        Set<RequestMethod> methods = requestMappingInfo.getMethodsCondition().getMethods();
        String httpMethod = methods.isEmpty() ? RequestMethod.GET.name() : methods.iterator().next().name();

        // 获取类级别的 Tag 注解
        Class<?> beanType = handlerMethod.getBeanType();
        Tag classTag = AnnotatedElementUtils.findMergedAnnotation(beanType, Tag.class);
        // tagName：name 为空则取 description
        String tagName = "";
        if (classTag != null) {
            tagName = classTag.name();
            if (tagName == null || tagName.isEmpty()) {
                tagName = classTag.description();
            }
        }

        // 获取方法级别的 Operation 注解
        Method method = handlerMethod.getMethod();
        Operation operation = AnnotatedElementUtils.findMergedAnnotation(method, Operation.class);
        // summary：summary 为空则取 description
        String summary = "";
        if (operation != null) {
            summary = operation.summary();
            if (summary == null || summary.isEmpty()) {
                summary = operation.description();
            }
        }

        // 获取请求参数类型
        String requestType = getRequestType(method);

        // 获取响应类型
        String responseType = getResponseType(method);

        // 根据部署模式获取模块信息和处理路径
        String path;
        BusinessModuleInfo moduleInfo;

        if (DeployUtils.isSingle()) {
            // 单机模式：取 path 第一个前缀作为模块标识，并去除该前缀
            String[] pathParts = originalPath.split("/");
            String modulePrefix = pathParts.length > 1 ? pathParts[1] : "";
            moduleInfo = moduleInfoMap.get(modulePrefix);
            // 去除第一个前缀
            path = pathParts.length > 2
                    ? "/" + String.join("/", Arrays.copyOfRange(pathParts, 2, pathParts.length))
                    : "/";
        } else {
            // 分布式模式：直接取第一个模块信息
            moduleInfo = moduleInfoMap.isEmpty() ? null : moduleInfoMap.values().iterator().next();
            path = originalPath;
        }

        String modulePrefix = moduleInfo != null ? moduleInfo.prefix() : "";


        boolean loginAllowAccess = beanType.isAnnotationPresent(LoginAllowAccess.class) || method.isAnnotationPresent(LoginAllowAccess.class);

        // === 表模型权限采集 ===
        List<TableModelInfo> tableModels = collectTableModelPermission(beanType, method, modulePrefix);

        return new ApiEndpointInfo(
                genId(modulePrefix, httpMethod, path),
                modulePrefix,
                tagName,
                path,
                httpMethod,
                summary,
                requestType,
                responseType,
                beanType.getName(),
                method.getName(),
                loginAllowAccess,
                tableModels   // 新增
        );
    }

    /**
     * 采集表模型权限配置
     */
    private List<TableModelInfo> collectTableModelPermission(Class<?> beanType, Method method, String modulePrefix) {
        TableModelPermission classAnnotation = AnnotatedElementUtils.findMergedAnnotation(beanType, TableModelPermission.class);
        TableModelPermission methodAnnotation = AnnotatedElementUtils.findMergedAnnotation(method, TableModelPermission.class);

        TableModelPermission effectiveAnnotation;
        if (methodAnnotation != null) {
            if (methodAnnotation.value().length == 0 && methodAnnotation.tables().length == 0) {
                return List.of();
            }
            effectiveAnnotation = methodAnnotation;
        } else if (classAnnotation != null) {
            effectiveAnnotation = classAnnotation;
        } else {
            return List.of();
        }

        String datasource = "master";

        List<TableModelInfo> result = new ArrayList<>();
        for (Class<?> domainClass : effectiveAnnotation.value()) {
            TableName tableNameAnnotation = domainClass.getAnnotation(TableName.class);
            if (tableNameAnnotation == null) {
                log.warn("@TableModelPermission 引用的类 {} 缺少 @TableName 注解，跳过", domainClass.getName());
                continue;
            }
            String tableName = tableNameAnnotation.value();
            Map<String, FieldPermission> fieldConfig = buildFieldConfig(domainClass);
            result.add(new TableModelInfo(modulePrefix, tableName, datasource, fieldConfig));
        }

        for (String tableName : effectiveAnnotation.tables()) {
            result.add(new TableModelInfo(modulePrefix, tableName, datasource, Map.of()));
        }

        return result;
    }

    private Map<String, FieldPermission> buildFieldConfig(Class<?> domainClass) {
        Map<String, FieldPermission> fieldConfig = new HashMap<>();
        for (java.lang.reflect.Field field : domainClass.getDeclaredFields()) {
            TableModelField fieldAnnotation = field.getAnnotation(TableModelField.class);
            if (fieldAnnotation == null) {
                continue;
            }
            String columnName = camelToUnderline(field.getName());
            fieldConfig.put(columnName, new FieldPermission(
                    fieldAnnotation.show(),
                    fieldAnnotation.desensitize(),
                    fieldAnnotation.strategy(),
                    fieldAnnotation.strategy() == SensitiveStrategy.CUSTOM ? fieldAnnotation.prefixNoMaskLen() : null,
                    fieldAnnotation.strategy() == SensitiveStrategy.CUSTOM ? fieldAnnotation.suffixNoMaskLen() : null,
                    fieldAnnotation.strategy() == SensitiveStrategy.CUSTOM ? fieldAnnotation.symbol() : null
            ));
        }
        return fieldConfig;
    }

    private String camelToUnderline(String camelCase) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String genId(String modulePrefix, String httpMethod, String httpUrl) {
        return MD5.create().digestHex("%s:%s:%s".formatted(modulePrefix, httpMethod, httpUrl));
    }

    /**
     * 获取请求参数类型
     */
    private String getRequestType(Method method) {
        Parameter[] parameters = method.getParameters();
        for (Parameter parameter : parameters) {
            // 跳过路径参数和请求参数
            if (parameter.isAnnotationPresent(PathVariable.class) ||
                    parameter.isAnnotationPresent(RequestParam.class)) {
                continue;
            }
            // 找到请求体参数
            if (parameter.isAnnotationPresent(RequestBody.class)) {
                return getTypeName(parameter.getParameterizedType());
            }
        }
        // 检查是否有普通对象参数（非基本类型）
        for (Parameter parameter : parameters) {
            Class<?> type = parameter.getType();
            if (!type.isPrimitive() &&
                    !type.getName().startsWith("java.lang") &&
                    !type.getName().startsWith("java.util") &&
                    !type.getName().startsWith("javax.") &&
                    !type.getName().startsWith("jakarta.") &&
                    !parameter.isAnnotationPresent(PathVariable.class) &&
                    !parameter.isAnnotationPresent(RequestParam.class)) {
                return getTypeName(parameter.getParameterizedType());
            }
        }
        return "";
    }

    /**
     * 获取响应类型
     */
    private String getResponseType(Method method) {
        Type returnType = method.getGenericReturnType();
        if (returnType == void.class) {
            return "";
        }
        return getTypeName(returnType);
    }

    /**
     * 获取类型名称（支持泛型）
     */
    private String getTypeName(Type type) {
        if (type instanceof Class<?> clazz) {
            return clazz.getName();
        } else if (type instanceof ParameterizedType parameterizedType) {
            StringBuilder sb = new StringBuilder();
            Type rawType = parameterizedType.getRawType();
            if (rawType instanceof Class<?> clazz) {
                sb.append(clazz.getName());
            } else {
                sb.append(rawType.getTypeName());
            }
            Type[] typeArgs = parameterizedType.getActualTypeArguments();
            if (typeArgs.length > 0) {
                sb.append("<");
                for (int i = 0; i < typeArgs.length; i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append(getTypeName(typeArgs[i]));
                }
                sb.append(">");
            }
            return sb.toString();
        }
        return type.getTypeName();
    }

    public record ApiEndpointWrapper(
            String applicationName,
            Map<String, List<ApiEndpointInfo>> endpoints
    ) {
    }

}

package org.quyq.gwsu.security.apiresource.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;
import org.quyq.gwsu.common.security.domain.ApiEndpointInfo;
import org.quyq.gwsu.security.api.apiresource.vo.ApiResourceVO;

import java.util.Objects;

/**
 * 接口资源表
 *
 * @author Quyq
 */
@Data
@Accessors(chain = true)
@TableName(value = "security_api_resource", autoResultMap = true)
@Schema(description = "接口资源表")
public class SecurityApiResource extends BaseDO {

    @TableId(type = IdType.INPUT)
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "模块前缀")
    private String modulePrefix;

    @Schema(description = "Tag标签名称")
    private String tagName;

    @Schema(description = "接口地址")
    private String reqPath;

    @Schema(description = "请求方式(GET/POST/PUT/DELETE等)")
    private String reqMethod;

    @Schema(description = "接口摘要")
    private String summary;

    @Schema(description = "请求参数类型全限定名")
    private String requestClass;

    @Schema(description = "响应类型全限定名")
    private String responseClass;

    @Schema(description = "类名")
    private String className;

    @Schema(description = "方法名")
    private String methodName;

    @Schema(description = "登录后允许访问")
    public Boolean loginAllowAccess;

    /**
     * DO 转 VO
     *
     * @return ApiResourceVO
     */
    public ApiResourceVO toVo() {
        ApiResourceVO vo = new ApiResourceVO();
        vo.setId(this.id);
        vo.setModulePrefix(this.modulePrefix);
        vo.setTagName(this.tagName);
        vo.setReqPath(this.reqPath);
        vo.setReqMethod(this.reqMethod);
        vo.setSummary(this.summary);
        vo.setLoginAllowAccess(this.loginAllowAccess);
        vo.copyBaseProperties(this);
        return vo;
    }

    public static SecurityApiResource EndpointInfo2Resource(ApiEndpointInfo endpointInfo) {
        SecurityApiResource resource = new SecurityApiResource();
        resource.setId(endpointInfo.id());
        resource.setModulePrefix(endpointInfo.modulePrefix());
        resource.setTagName(endpointInfo.tagName());
        resource.setReqPath(endpointInfo.reqPath());
        resource.setReqMethod(endpointInfo.reqMethod());
        resource.setSummary(endpointInfo.summary());
        resource.setClassName(endpointInfo.className());
        resource.setMethodName(endpointInfo.methodName());
        resource.setRequestClass(endpointInfo.requestClass());
        resource.setResponseClass(endpointInfo.responseClass());
        resource.setLoginAllowAccess(endpointInfo.allowLoginAccess());
        return resource;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SecurityApiResource that = (SecurityApiResource) o;
        return Objects.equals(id, that.id) && Objects.equals(modulePrefix, that.modulePrefix) && Objects.equals(tagName, that.tagName) && Objects.equals(reqPath, that.reqPath) && Objects.equals(reqMethod, that.reqMethod) && Objects.equals(summary, that.summary) && Objects.equals(requestClass, that.requestClass) && Objects.equals(responseClass, that.responseClass) && Objects.equals(className, that.className) && Objects.equals(methodName, that.methodName) && Objects.equals(loginAllowAccess, that.loginAllowAccess);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, modulePrefix, tagName, reqPath, reqMethod, summary, requestClass, responseClass, className, methodName , loginAllowAccess);
    }
}

package com.dtt.base.common.log.aspect;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.filter.PropertyFilter;
import com.dtt.base.common.core.constant.Constants;
import com.dtt.base.common.core.domain.visitor.Visitor;
import com.dtt.base.common.core.utils.spring.ProxyUtil;
import com.dtt.base.common.core.utils.spring.ServletUtils;
import com.dtt.base.common.core.utils.text.StringUtil;
import com.dtt.base.common.log.config.LogProperties;
import com.dtt.base.common.log.service.AccessLogService;
import com.dtt.base.common.core.logback.OperLogContainer;
import com.dtt.base.common.security.domain.LoginUser;
import com.dtt.base.common.security.enums.TerminalType;
import com.dtt.base.common.security.utils.SecurityUtils;
import com.dtt.base.log.api.domain.CloudseaOperLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.util.AntPathMatcher;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class LogAspectInterceptor implements MethodInterceptor{

    private static final String ARGS_KEY = "args";

    public LogAspectInterceptor(LogProperties.AccessLogProperties logProperties , String applicationName){
        this.logProperties = logProperties;
        this.applicationName = applicationName;
    }
    private final LogProperties.AccessLogProperties logProperties;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final String applicationName;

    @Setter
    private AccessLogService logService;


    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {

        //如果日志功能没有启用或不是对外接口不记录
        if(Boolean.FALSE.equals(logProperties.getEnabled()) ||
                Objects.nonNull(invocation.getMethod().getAnnotation(InitBinder.class))){
            return invocation.proceed();
        }

        //如果是指定忽略的服务或者没有请求体不记录
        HttpServletRequest request = ServletUtils.getRequest();
        if(matchServer() || Objects.isNull(request)){
            return invocation.proceed();
        }

        if(StringUtil.isNotEmpty(logProperties.getIgnoreList())){
            String uri = request.getRequestURI();
            String method = request.getMethod();
            //如果是指定忽略的接口，则跳过
            if(logProperties.getIgnoreList().stream().anyMatch(i ->{
                if(CharSequenceUtil.isBlank(i)){
                    return false;
                }

                String[] urls = i.split(" ");
                return matchIgnore(urls , uri , method);
            })){
                return invocation.proceed();
            }

        }


        Object result;
        CloudseaOperLog log = createLog(invocation , request);
        //推送请求日志
        this.put(log);
        try {
            result = invocation.proceed();
            setResponse(log , result , null);
        }catch (Throwable ex){
            setResponse(log , null , ex);
            throw ex;
        }finally {
            //推送响应日志
            this.put(log);
            OperLogContainer.remove();
        }

        return result;
    }


    private boolean matchServer(){
        if(CharSequenceUtil.isBlank(this.applicationName))
            return false;

        return logProperties.getIgnoreServer().contains(this.applicationName);
    }

    private boolean matchIgnore(String[] ignoreUrl , String uri , String method){
        String configMethod = null;
        String configUri;
        if(ignoreUrl.length< 2){
            configUri = ignoreUrl[0];
        }else {
            configMethod = ignoreUrl[0];
            configUri = ignoreUrl[1];
        }

        if(Objects.nonNull(configMethod)){
            boolean match = !configMethod.contains("*") && !configMethod.equalsIgnoreCase(method);
            if(match){
                return false;
            }
        }

        return pathMatcher.match(configUri , uri);
    }


    /**
     * 生成返回数据
     * @param loger
     * @param result
     * @param ex
     */
    private void setResponse(CloudseaOperLog loger , Object result , Throwable ex){
        try {
            LocalDateTime nowDate = LocalDateTime.now();
            loger.setResponseTime(nowDate).setModifyDate(nowDate);
            loger.setConsumeMill(Duration.between(loger.getRequestTime(),nowDate).toMillis())
                    .setModifyOp(ServletUtils.getHeaders().get(Constants.Feign.LOGIN_USER_IDENTIFIER));

            if(Objects.nonNull(ex)){
                loger.setErrorMsg(ExceptionUtil.stacktraceToString(ex , 5000));
                return;
            }
            loger.setStatus(0);

            if(Objects.nonNull(result)){
                loger.setResponseData(JSON.toJSONString(result));
            }
        }catch (Exception e){
            //捕获日志记录操作异常 ， 防止日志记录功能影响正常业务接口
            log.error("日志记录异常（不影响业务响应）" ,e);
        }finally {
            if(Objects.nonNull(loger))
                log.info("[接口响应]-\n┌───接口响应─────────────" +
                                "\n├── 响应结果： {} \n├── {} \n├── 接口耗时： {}ms " +
                                "\n└───────────────────────" ,Objects.isNull(ex) ? "成功" : "异常" ,
                    Objects.isNull(ex) ?
                            StringUtil.format("响应数据： {}" , loger.getResponseData())
                    :
                            StringUtil.format("错误信息： {}" , ex.getMessage()) , loger.getConsumeMill()
                    );
        }



    }

    /**
     * 生成日志文件
     * @param invocation
     * @param request
     * @return
     */
    private CloudseaOperLog createLog(MethodInvocation invocation , HttpServletRequest request){
        CloudseaOperLog accessLog = new CloudseaOperLog();
        LocalDateTime now = LocalDateTime.now();

        JSONObject requestParam = getRequestParam(request, invocation);

        accessLog.setOperId(OperLogContainer.put());
        accessLog.setRequestTime(now)
                //默认失败状态
                .setStatus(1)
                .setMethod(getMethodName(invocation))
                .setRequestMethod(request.getMethod().toUpperCase())
                .setRequestUrl(request.getRequestURI())
                .setRequestParam(requestParam.toJSONString())
                .setApplication(this.applicationName)
                .setCreateDate(now);

        Map<String, String> headers = ServletUtils.getHeaders();
        accessLog.setTid(headers.get(Constants.Feign.LOG_CONTEXT_FLAG))
                .setParentId(headers.get(Constants.Log.SERVER_LOG_PARENT_ID))
                .setFromApp(headers.get(Constants.Log.SERVER_LOG_FROM_APP))
                .setMenuId(headers.get(Constants.Log.VIEW_MENU_INFO))
                .setTokenId(getTokenId(headers))
                .setOperName(headers.get(Constants.Feign.LOGIN_USER_IDENTIFIER))
                .setTerminalDetail(headers.get("user-agent"))
                .setCreateOp(headers.get(Constants.Feign.LOGIN_USER_IDENTIFIER));

        setApiDescription(invocation , accessLog);

        //终端赋值
        if(ProxyUtil.hasClass("com.dtt.base.common.security.utils.SecurityUtils")){
            LoginUser<Visitor> loginUser = SecurityUtils.getLoginUser();
            if(Objects.nonNull(loginUser)){
                TerminalType terminal = loginUser.getTerminal();
                accessLog.setTerminal(TerminalType.APP == terminal ? "MOBILE" : "WEB");
            }
        }
        if(!StringUtil.hasText(accessLog.getTerminal())){
            UserAgent userAgent = UserAgentUtil.parse(accessLog.getTerminalDetail());
            if(Objects.nonNull(userAgent)){
                accessLog.setTerminal(userAgent.isMobile() ? "MOBILE" : "WEB");
            }
        }

        log.info("[接口调用]-\n┌───接口调用─────────────" +
                        "\n├── 路径： {}:{} \n├── 来源： {} \n├── 入参： {} " +
                        "\n└───────────────────────" , request.getMethod().toUpperCase() ,request.getRequestURI() ,
                StringUtil.format("{}端 - {}" ,accessLog.getTerminal() , accessLog.getOperName()) ,
                CollUtil.isEmpty(requestParam.getJSONObject(ARGS_KEY)) ? "无" : requestParam.getJSONObject(ARGS_KEY).toJSONString());

        return accessLog;

    }

    private void setApiDescription(MethodInvocation invocation, CloudseaOperLog log){
        if(ProxyUtil.hasClass("io.swagger.v3.oas.annotations.Operation")){
            Operation operation = invocation.getMethod().getAnnotation(Operation.class);
            Tag apiAnn = invocation.getMethod().getDeclaringClass().getAnnotation(Tag.class);
            if(Objects.nonNull(operation)){
                log.setApiDescription(operation.summary());
            }
            if(Objects.nonNull(apiAnn)){
                log.setApiModule(apiAnn.name());
            }
        }

    }

    private String getMethodName(MethodInvocation invocation){
        return CharSequenceUtil.format("{}.{}()" , invocation.getThis().getClass().getName() , invocation.getMethod().getName());
    }

    private String getTokenId(Map<String, String> headers){
        String token = headers.get(Constants.Feign.LOGIN_USER_TOKEN);
        if(CharSequenceUtil.isBlank(token))
            return null;

        return token.replace(Constants.User.TOKEN_PREFIX , "").trim();

    }

    private JSONObject getRequestParam(HttpServletRequest request,MethodInvocation invocation){
        Object[] arguments = invocation.getArguments();
        JSONObject allParams = new JSONObject();


        allParams.put(ARGS_KEY , new JSONObject());
        for (int i = 0 ; i < arguments.length ; i ++ ){
            Object param = arguments[i];
            if(Objects.isNull(param) || isFilterObject(param))
                continue;
            try{
                PropertyFilter filter = (obj , name , value) ->{
                    if(Objects.isNull(value)){
                        return true;
                    }
                    return !isFilterObject(value);
                };

                allParams.getJSONObject(ARGS_KEY).put(String.valueOf(i) ,JSON.parseObject(JSON.toJSONString(param , filter)) );
            }catch (Exception ex){
                allParams.getJSONObject(ARGS_KEY).put(String.valueOf(i) , String.valueOf(param));
            }


        }

        Map<String, String[]> param = request.getParameterMap();
        allParams.put("query" , param);

        return allParams;
    }

    public boolean isFilterObject(final Object o) {
        Class<?> clazz = o.getClass();
        if (clazz.isArray()) {
            return clazz.getComponentType().isAssignableFrom(MultipartFile.class);
        } else if (Collection.class.isAssignableFrom(clazz) ) {
            Collection<?> collection = (Collection<?>) o;
            Iterator<?> iter = collection.iterator();
            return !collection.isEmpty() &&  iter.next() instanceof MultipartFile;
        } else if (Map.class.isAssignableFrom(clazz)) {
            Map<?,?> map = (Map<?,?>) o;
            if(map.isEmpty()){
                return false;
            }
            Iterator<?> iter = map.entrySet().iterator();
            Map.Entry<?,?> entry = (Map.Entry<?,?>) iter.next();
            return entry.getValue() instanceof MultipartFile;
        }
        return o instanceof MultipartFile || o instanceof HttpServletRequest || o instanceof HttpServletResponse
                || o instanceof BindingResult;
    }

    private void put(CloudseaOperLog log){
        logService.saveSysLog(log);
    }

}

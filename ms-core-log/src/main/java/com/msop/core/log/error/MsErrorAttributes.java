package com.msop.core.log.error;

import com.msop.core.common.model.Result;
import com.msop.core.common.utils.BeanUtil;
import com.msop.core.log.publisher.AuditErrorLogPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

/**
 * 全局异常处理
 *
 * @author ruozhuliufeng
 */
@Slf4j
public class MsErrorAttributes extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
        String requestUri = this.getAttr(webRequest, "javax.servlet.error.request_uri");
        Integer status = this.getAttr(webRequest, "javax.servlet.error.status_code");
        Throwable error = getError(webRequest);
        Result result;
        if (error == null) {
            log.error("URL:{} error status:{}", requestUri, status);
            result = Result.failed("系统未知异常[HttpStatus]:" + status);
        } else {
            log.error(String.format("URL:%s error status:%d", requestUri, status), error);
            result = Result.failed(status, error.getMessage());
        }
        // 发送服务异常事件
        AuditErrorLogPublisher.publishEvent(error, requestUri);
        return BeanUtil.toMap(result);
    }

    private <T> T getAttr(WebRequest request, String name) {
        return (T) request.getAttribute(name, RequestAttributes.SCOPE_REQUEST);
    }
}
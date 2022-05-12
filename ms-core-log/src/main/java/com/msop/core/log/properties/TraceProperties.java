package com.msop.core.log.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 日志链路追踪配置
 *
 * @author ruozhuliufeng
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ms.trace")
public class TraceProperties {

    /**
     * 是否开启日志链路追踪
     */
    private Boolean enable = false;
}

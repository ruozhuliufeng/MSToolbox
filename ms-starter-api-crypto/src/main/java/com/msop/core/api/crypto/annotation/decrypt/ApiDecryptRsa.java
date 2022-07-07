package com.msop.core.api.crypto.annotation.decrypt;

import com.msop.core.api.crypto.enums.CryptoType;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * <p>RSA解密含有{@link org.springframework.web.bind.annotation.RequestBody}注解的参数请求数据</p>
 *
 * @author ruozhuilufeng
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ApiDecrypt(CryptoType.RSA)
public @interface ApiDecryptRsa {

}

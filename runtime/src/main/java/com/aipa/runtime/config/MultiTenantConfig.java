package com.aipa.runtime.config;

import com.aipa.runtime.context.ProjectContextInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MultiTenantConfig — 多租戶架構配置
 *
 * 註冊必要的過濾器和攔截器，確保每個請求都能正確設置和清理項目上下文。
 */
@Configuration
public class MultiTenantConfig {

    @Autowired
    private ProjectContextInterceptor projectContextInterceptor;

    /**
     * 註冊 ProjectContextInterceptor 為 Servlet Filter
     *
     * 它必須在所有其他過濾器之後執行，確保為下游業務邏輯提供項目上下文。
     */
    @Bean
    public FilterRegistrationBean<ProjectContextInterceptor> projectContextFilter() {
        FilterRegistrationBean<ProjectContextInterceptor> bean =
            new FilterRegistrationBean<>(projectContextInterceptor);

        // 設置過濾器順序（越低越先執行）
        bean.setOrder(1);

        // 指定攔截路徑
        bean.addUrlPatterns("/api/v1/*");
        bean.addUrlPatterns("/api/v2/*");

        return bean;
    }
}


package com.aipa.runtime.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * IP 白名單安全過濾器
 * 階段 9: 企業級安全強化 - 限制 API 訪問來源 IP
 *
 * 功能:
 * - 支援多個 IP 地址配置（逗号分隔）
 * - 支援 IP 段 (CIDR 格式)
 * - 允許本地測試（127.0.0.1）
 * - 拒絕未授權 IP 並記錄審計日誌
 */
@Component
public class IpWhitelistFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(IpWhitelistFilter.class);

    @Value("${aipa.security.ip-whitelist:}")
    private String ipWhitelistConfig;

    @Value("${aipa.security.enable-ip-whitelist:false}")
    private boolean enableIpWhitelist;

    private List<IpMatcher> matchers = new ArrayList<>();

    @Override
    public void init(FilterConfig config) throws ServletException {
        if (enableIpWhitelist && ipWhitelistConfig != null && !ipWhitelistConfig.trim().isEmpty()) {
            parseWhitelist(ipWhitelistConfig);
            logger.info("IP 白名單已啟用，已配置 {} 條規則", matchers.size());
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!enableIpWhitelist || matchers.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String clientIp = getClientIp(httpRequest);

        if (isIpAllowed(clientIp)) {
            logger.debug("IP {} 已授權", clientIp);
            chain.doFilter(request, response);
        } else {
            logger.warn("IP {} 被 IP 白名單過濾器拒絕，請求路径 {}", clientIp, httpRequest.getRequestURI());
            httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
            httpResponse.setContentType("application/json;charset=UTF-8");
            httpResponse.getWriter().write("{\"error\": \"存取被拒: IP 不在白名單中\"}");
        }
    }

    @Override
    public void destroy() {
        matchers.clear();
    }

    /**
     * 解析白名單配置
     * 支援格式: "192.168.1.1,192.168.1.0/24,10.0.0.0/8"
     */
    private void parseWhitelist(String config) {
        String[] entries = config.split(",");
        for (String entry : entries) {
            entry = entry.trim();
            if (entry.isEmpty()) continue;

            if (entry.contains("/")) {
                // CIDR 格式
                matchers.add(new CidrMatcher(entry));
            } else {
                // 單一 IP
                matchers.add(new IpExactMatcher(entry));
            }
        }
    }

    /**
     * 檢查 IP 是否在白名單中
     */
    private boolean isIpAllowed(String ip) {
        // 本地測試環境總是允許
        if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            return true;
        }

        for (IpMatcher matcher : matchers) {
            if (matcher.matches(ip)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 取得客戶端真實 IP
     * 支援後方 Proxy (X-Forwarded-For, X-Real-IP)
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty()) {
            // X-Forwarded-For 可能包含多個 IP，取第一個
            ip = ip.split(",")[0].trim();
        }

        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }

        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }

        return ip;
    }

    // IP 匹配器接口
    interface IpMatcher {
        boolean matches(String ip);
    }

    // 精確 IP 匹配
    static class IpExactMatcher implements IpMatcher {
        private final String ip;

        IpExactMatcher(String ip) {
            this.ip = ip;
        }

        @Override
        public boolean matches(String clientIp) {
            return ip.equals(clientIp);
        }
    }

    // CIDR 段匹配 (簡化實現)
    static class CidrMatcher implements IpMatcher {
        private final String address;
        private final int network;
        private final int mask;

        CidrMatcher(String cidr) {
            String[] cidrParts = cidr.split("/");
            this.address = cidrParts[0];
            int maskBits = Integer.parseInt(cidrParts[1]);
            this.network = ipToInt(address);
            // 建立掩碼：例如 /24 = 11111111.11111111.11111111.00000000
            // 用 32 - maskBits 得到右邊零的數量
            this.mask = maskBits == 0 ? 0 : (0xFFFFFFFF << (32 - maskBits));
        }

        @Override
        public boolean matches(String clientIp) {
            try {
                int clientIpInt = ipToInt(clientIp);
                return (clientIpInt & mask) == (network & mask);
            } catch (Exception e) {
                return false;
            }
        }

        private static int ipToInt(String ip) {
            String[] parts = ip.split("\\.");
            if (parts.length != 4) throw new IllegalArgumentException("Invalid IP: " + ip);

            int result = 0;
            for (int i = 0; i < 4; i++) {
                int part = Integer.parseInt(parts[i]);
                if (part < 0 || part > 255) throw new IllegalArgumentException("Invalid IP part: " + part);
                result = (result << 8) | part;
            }
            return result;
        }
    }
}


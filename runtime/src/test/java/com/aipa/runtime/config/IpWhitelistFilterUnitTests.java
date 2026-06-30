package com.aipa.runtime.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 9 第二切片 — IP 白名單單元測試（不依賴 Spring Context）
 *
 * 直接測試 IpWhitelistFilter 中的 IP 匹配邏輯
 */
class IpWhitelistFilterUnitTests {

    /**
     * 測試精確 IP 匹配
     */
    @Test
    void testExactIpMatcher() {
        IpWhitelistFilter.IpExactMatcher matcher =
            new IpWhitelistFilter.IpExactMatcher("192.168.1.100");

        assertTrue(matcher.matches("192.168.1.100"));
        assertFalse(matcher.matches("192.168.1.101"));
        assertFalse(matcher.matches("10.0.0.1"));
    }

    /**
     * 測試 CIDR 段匹配 (/24)
     */
    @Test
    void testCidrMatcher24() {
        IpWhitelistFilter.CidrMatcher matcher =
            new IpWhitelistFilter.CidrMatcher("192.168.1.0/24");

        // 應該匹配
        assertTrue(matcher.matches("192.168.1.0"));
        assertTrue(matcher.matches("192.168.1.1"));
        assertTrue(matcher.matches("192.168.1.100"));
        assertTrue(matcher.matches("192.168.1.255"));

        // 不應該匹配
        assertFalse(matcher.matches("192.168.0.255"));
        assertFalse(matcher.matches("192.168.2.0"));
        assertFalse(matcher.matches("10.0.0.1"));
    }

    /**
     * 測試 CIDR 段匹配 (/16)
     */
    @Test
    void testCidrMatcher16() {
        IpWhitelistFilter.CidrMatcher matcher =
            new IpWhitelistFilter.CidrMatcher("10.0.0.0/16");

        // 應該匹配
        assertTrue(matcher.matches("10.0.0.0"));
        assertTrue(matcher.matches("10.0.1.1"));
        assertTrue(matcher.matches("10.0.255.255"));

        // 不應該匹配
        assertFalse(matcher.matches("10.1.0.0"));
        assertFalse(matcher.matches("9.255.255.255"));
        assertFalse(matcher.matches("192.168.1.1"));
    }

    /**
     * 測試 CIDR 段匹配 (/8) —  所有第一字節為 172 的 IP
     */
    @Test
    void testCidrMatcher8() {
        IpWhitelistFilter.CidrMatcher matcher =
            new IpWhitelistFilter.CidrMatcher("172.16.0.0/8");

        // 應該匹配
        assertTrue(matcher.matches("172.16.0.0"));
        assertTrue(matcher.matches("172.100.100.100"));
        assertTrue(matcher.matches("172.255.255.255"));
        // /8 對於 172.x.x.x 也應該匹配
        assertTrue(matcher.matches("172.0.0.0"));

        // 不應該匹配
        assertFalse(matcher.matches("171.255.255.255"));
        assertFalse(matcher.matches("173.0.0.0"));
        assertFalse(matcher.matches("10.0.0.0"));
    }

    /**
     * 測試 CIDR 段匹配 (/32) — 等同精確匹配
     */
    @Test
    void testCidrMatcher32() {
        IpWhitelistFilter.CidrMatcher matcher =
            new IpWhitelistFilter.CidrMatcher("192.168.1.100/32");

        // 應該匹配
        assertTrue(matcher.matches("192.168.1.100"));

        // 不應該匹配
        assertFalse(matcher.matches("192.168.1.99"));
        assertFalse(matcher.matches("192.168.1.101"));
    }

    /**
     * 測試無效 IP（CIDR 匹配器應優雅處理）
     */
    @Test
    void testInvalidIpHandling() {
        IpWhitelistFilter.CidrMatcher matcher =
            new IpWhitelistFilter.CidrMatcher("192.168.1.0/24");

        // 無效格式應返回 false，不應拋出異常
        assertFalse(matcher.matches("invalid"));
        assertFalse(matcher.matches("192.168.1"));
        assertFalse(matcher.matches("192.168.1.1.1"));
        assertFalse(matcher.matches(""));
    }

    /**
     * 測試本地 IP 識別
     */
    @Test
    void testLocalhostIps() {
        IpWhitelistFilter.IpExactMatcher localhost =
            new IpWhitelistFilter.IpExactMatcher("127.0.0.1");
        IpWhitelistFilter.IpExactMatcher ipv6Localhost =
            new IpWhitelistFilter.IpExactMatcher("::1");

        assertTrue(localhost.matches("127.0.0.1"));
        assertFalse(localhost.matches("127.0.0.2"));

        assertTrue(ipv6Localhost.matches("::1"));
        assertFalse(ipv6Localhost.matches("::2"));
    }

    /**
     * 測試常見私有 IP 段
     */
    @Test
    void testPrivateIpRanges() {
        // 10.0.0.0/8
        IpWhitelistFilter.CidrMatcher range1 =
            new IpWhitelistFilter.CidrMatcher("10.0.0.0/8");
        assertTrue(range1.matches("10.0.0.0"));
        assertTrue(range1.matches("10.255.255.255"));
        assertFalse(range1.matches("11.0.0.0"));

        // 172.16.0.0/12
        IpWhitelistFilter.CidrMatcher range2 =
            new IpWhitelistFilter.CidrMatcher("172.16.0.0/12");
        assertTrue(range2.matches("172.16.0.0"));
        assertTrue(range2.matches("172.31.255.255"));
        assertFalse(range2.matches("172.15.255.255"));
        assertFalse(range2.matches("172.32.0.0"));

        // 192.168.0.0/16
        IpWhitelistFilter.CidrMatcher range3 =
            new IpWhitelistFilter.CidrMatcher("192.168.0.0/16");
        assertTrue(range3.matches("192.168.0.0"));
        assertTrue(range3.matches("192.168.255.255"));
        assertFalse(range3.matches("192.167.255.255"));
        assertFalse(range3.matches("192.169.0.0"));
    }
}


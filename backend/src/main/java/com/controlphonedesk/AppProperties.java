package com.controlphonedesk;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private final Adb adb = new Adb();
    private final Scrcpy scrcpy = new Scrcpy();
    private final Cors cors = new Cors();
    private final Security security = new Security();

    /** adb 相关配置（如可执行文件路径） */
    public Adb getAdb() {
        return adb;
    }

    /** scrcpy server 相关配置（版本、端口、jar 路径等） */
    public Scrcpy getScrcpy() {
        return scrcpy;
    }

    /** CORS 允许的来源配置 */
    public Cors getCors() {
        return cors;
    }

    /** 安全配置（JWT 等） */
    public Security getSecurity() {
        return security;
    }

    public static class Adb {
        private String bin = "adb";

        /** adb 可执行文件路径 */
        public String getBin() {
            return bin;
        }

        public void setBin(String bin) {
            this.bin = bin;
        }
    }

    public static class Scrcpy {
        private String serverVersion = "1.19-ws5";
        private int serverPort = 8886;
        private String logLevel = "ERROR";
        private String serverPackage = "com.genymobile.scrcpy.Server";
        private String serverJarPath = "scrcpy/scrcpy-server.jar";
        private boolean listenOnAllInterfaces = false;

        /** scrcpy server 版本号（需与 jar 内一致） */
        public String getServerVersion() {
            return serverVersion;
        }

        public void setServerVersion(String serverVersion) {
            this.serverVersion = serverVersion;
        }

        /** scrcpy server 端口（设备侧监听端口） */
        public int getServerPort() {
            return serverPort;
        }

        public void setServerPort(int serverPort) {
            this.serverPort = serverPort;
        }

        /** scrcpy server 日志级别 */
        public String getLogLevel() {
            return logLevel;
        }

        public void setLogLevel(String logLevel) {
            this.logLevel = logLevel;
        }

        /** scrcpy server 主类包名 */
        public String getServerPackage() {
            return serverPackage;
        }

        public void setServerPackage(String serverPackage) {
            this.serverPackage = serverPackage;
        }

        /** scrcpy server jar 在 classpath 下的路径 */
        public String getServerJarPath() {
            return serverJarPath;
        }

        public void setServerJarPath(String serverJarPath) {
            this.serverJarPath = serverJarPath;
        }

        /** 是否监听所有网卡（true/false） */
        public boolean isListenOnAllInterfaces() {
            return listenOnAllInterfaces;
        }

        public void setListenOnAllInterfaces(boolean listenOnAllInterfaces) {
            this.listenOnAllInterfaces = listenOnAllInterfaces;
        }
    }

    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>();
        private List<String> allowedOriginPatterns = new ArrayList<>();

        /** 允许跨域访问的前端源地址 */
        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }

        /** 允许跨域访问的来源模式（支持通配符） */
        public List<String> getAllowedOriginPatterns() {
            return allowedOriginPatterns;
        }

        public void setAllowedOriginPatterns(List<String> allowedOriginPatterns) {
            this.allowedOriginPatterns = allowedOriginPatterns;
        }
    }

    public static class Security {
        private final Jwt jwt = new Jwt();

        public Jwt getJwt() {
            return jwt;
        }
    }

    public static class Jwt {
        private String secret = "change-me";
        private long expirationMinutes = 720;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getExpirationMinutes() {
            return expirationMinutes;
        }

        public void setExpirationMinutes(long expirationMinutes) {
            this.expirationMinutes = expirationMinutes;
        }
    }
}

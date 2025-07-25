package com.ai.coder;

import com.ai.coder.config.AppProperties;
import com.ai.coder.util.BrowserUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

/**
 * 主要功能：
 * 1. 文件读取、写入、编辑
 * 2. 目录列表和结构查看
 * 4. 连续性文件操作
 */
@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
@EnableAspectJAutoProxy
public class AICoderApplication {

    private static final Logger logger = LoggerFactory.getLogger(AICoderApplication.class);

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private Environment environment;

    public static void main(String[] args) {
        SpringApplication.run(AICoderApplication.class, args);
    }

    /**
     * 应用启动完成后的事件监听器
     * 自动打开浏览器访问应用首页
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        AppProperties.Browser browserConfig = appProperties.getBrowser();

        if (!browserConfig.isAutoOpen()) {
            logger.info("浏览器自动打开已禁用");
            return;
        }

        // 获取实际的服务器端口
        String port = environment.getProperty("server.port", "8080");
        String actualUrl = browserConfig.getUrl().replace("${server.port:8080}", port);

        logger.info("应用启动完成!");
        logger.info("准备 {} 秒后打开浏览器", browserConfig.getDelaySeconds());

        // 在新线程中延迟打开浏览器，避免阻塞主线程
        new Thread(() -> {
            try {
                Thread.sleep(browserConfig.getDelaySeconds() * 1000L);

                if (BrowserUtil.isValidUrl(actualUrl)) {
                    boolean success = BrowserUtil.openBrowser(actualUrl);
                    if (success) {
                        logger.info("✅ Browser opened successfully: {}", actualUrl);
                        System.out.println("🌐 Web interface opened: " + actualUrl);
                    } else {
                        logger.warn("❌ Failed to open browser automatically");
                        System.out.println("⚠️  Please manually open: " + actualUrl);
                    }
                } else {
                    logger.error("❌ Invalid URL: {}", actualUrl);
                    System.out.println("⚠️  Invalid URL configured: " + actualUrl);
                }

            } catch (InterruptedException e) {
                logger.warn("Browser opening was interrupted", e);
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}

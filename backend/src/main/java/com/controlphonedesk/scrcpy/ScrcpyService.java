package com.controlphonedesk.scrcpy;

import com.controlphonedesk.AppProperties;
import com.controlphonedesk.adb.AdbService;
import com.controlphonedesk.adb.ProcessRunner;
import com.controlphonedesk.adb.ProcessResult;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class ScrcpyService {
    private static final String DEVICE_JAR_PATH = "/data/local/tmp/scrcpy-server.jar";
    private static final String PID_FILE = "/data/local/tmp/ws_scrcpy.pid";
    private static final Duration START_TIMEOUT = Duration.ofSeconds(5);

    private final AppProperties properties;
    private final AdbService adbService;

    public ScrcpyService(AppProperties properties, AdbService adbService) {
        this.properties = properties;
        this.adbService = adbService;
    }

    /**
     * 确保 scrcpy server 在设备端运行。
     * 如果已启动则直接返回 PID，否则 push jar 并启动。
     */
    public int ensureServerRunning(String udid) throws IOException, InterruptedException {
        return ensureServerRunning(udid, false);
    }

    /**
     * 确保 scrcpy server 在设备端运行（可强制重启）。
     */
    public int ensureServerRunning(String udid, boolean forceRestart) throws IOException, InterruptedException {
        if (forceRestart) {
            killServers(udid);
        }
        Optional<Integer> existing = getServerPid(udid);
        if (existing.isPresent()) {
            return existing.get();
        }
        pushServerJar(udid);
        startServer(udid);
        return waitForServer(udid);
    }

    /**
     * 通过解析 /proc/<pid>/cmdline 判断 scrcpy server 是否在运行。
     */
    public Optional<Integer> getServerPid(String udid) throws IOException, InterruptedException {
        List<Integer> pids = listAppProcessPids(udid);
        for (Integer pid : pids) {
            String cmdline = readCmdline(udid, pid);
            if (isScrcpyServerCmdline(cmdline)) {
                return Optional.of(pid);
            }
        }
        return Optional.empty();
    }

    /**
     * 尝试列出 app_process 的 PID（不同 Android 版本命令差异）。
     */
    private List<Integer> listAppProcessPids(String udid) throws IOException, InterruptedException {
        List<Integer> pids = new ArrayList<>();
        String output = shell(udid, "pidof app_process");
        if (!output.isBlank()) {
            for (String token : output.trim().split("\\s+")) {
                try {
                    pids.add(Integer.parseInt(token));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (!pids.isEmpty()) {
            return pids;
        }
        output = shell(udid, "ps -A | grep app_process");
        for (String line : output.split("\n")) {
            String[] parts = line.trim().split("\\s+");
            if (parts.length >= 2) {
                try {
                    pids.add(Integer.parseInt(parts[1]));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return pids;
    }

    /**
     * 读取进程 cmdline。
     */
    private String readCmdline(String udid, int pid) throws IOException, InterruptedException {
        return shell(udid, "cat /proc/" + pid + "/cmdline");
    }

    /**
     * 判断 cmdline 是否为目标 scrcpy server（匹配包名+版本）。
     */
    private boolean isScrcpyServerCmdline(String cmdline) {
        if (cmdline == null || cmdline.isBlank()) {
            return false;
        }
        String cleaned = cmdline.replace('\u0000', ' ').trim();
        if (cleaned.isBlank()) {
            return false;
        }
        String[] tokens = cleaned.split("\\s+");
        String serverPackage = properties.getScrcpy().getServerPackage();
        String serverVersion = properties.getScrcpy().getServerVersion();
        for (int i = 0; i < tokens.length - 1; i++) {
            if (serverPackage.equals(tokens[i]) && serverVersion.equals(tokens[i + 1])) {
                return true;
            }
        }
        return false;
    }

    /**
     * 杀掉设备端所有 scrcpy server 进程（用于强制重启）。
     */
    private void killServers(String udid) throws IOException, InterruptedException {
        List<Integer> pids = listAppProcessPids(udid);
        for (Integer pid : pids) {
            String cmdline = readCmdline(udid, pid);
            if (isScrcpyServerCmdline(cmdline)) {
                adbService.shell(udid, "kill " + pid);
            }
        }
        // 清理旧 PID 文件，避免读到过期 PID
        adbService.shell(udid, "rm -f " + PID_FILE);
        // 给系统一点时间回收进程
        TimeUnit.MILLISECONDS.sleep(300);
    }

    /**
     * 将 jar 从资源目录解压到临时目录，并 push 到设备。
     */
    private void pushServerJar(String udid) throws IOException, InterruptedException {
        Path jarPath = extractServerJar();
        adbService.pushFile(udid, jarPath.toString(), DEVICE_JAR_PATH);
    }

    /**
     * 从 classpath 提取 scrcpy server jar 到本机临时目录（避免重复拷贝）。
     */
    private Path extractServerJar() throws IOException {
        Path targetDir = Paths.get(System.getProperty("java.io.tmpdir"), "ws-scrcpy");
        Files.createDirectories(targetDir);
        Path target = targetDir.resolve("scrcpy-server.jar");
        ClassPathResource resource = new ClassPathResource(properties.getScrcpy().getServerJarPath());
        long resourceSize;
        try {
            resourceSize = resource.contentLength();
        } catch (IOException ex) {
            resourceSize = -1;
        }
        if (Files.exists(target) && (resourceSize <= 0 || Files.size(target) == resourceSize)) {
            return target;
        }
        try (InputStream inputStream = resource.getInputStream()) {
            Files.copy(inputStream, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }

    /**
     * 通过 adb shell 在设备端启动 scrcpy server。
     */
    private void startServer(String udid) throws IOException, InterruptedException {
        String version = properties.getScrcpy().getServerVersion();
        String logLevel = properties.getScrcpy().getLogLevel();
        int port = properties.getScrcpy().getServerPort();
        String serverPackage = properties.getScrcpy().getServerPackage();
        boolean listenOnAll = properties.getScrcpy().isListenOnAllInterfaces();
        String args = String.join(
            " ",
            version,
            "web",
            logLevel,
            Integer.toString(port),
            Boolean.toString(listenOnAll)
        );
        String command = "CLASSPATH=" + DEVICE_JAR_PATH
            + " nohup app_process / " + serverPackage + " " + args
            + " >/dev/null 2>&1 &";
        adbService.shell(udid, command);
    }

    /**
     * 等待 scrcpy server 启动成功（通过 PID 文件或进程列表）。
     */
    private int waitForServer(String udid) throws IOException, InterruptedException {
        long deadline = System.nanoTime() + START_TIMEOUT.toNanos();
        int attempt = 0;
        while (System.nanoTime() < deadline) {
            attempt++;
            Integer pid = readPidFile(udid).orElseGet(() -> {
                try {
                    return getServerPid(udid).orElse(null);
                } catch (Exception ignored) {
                    return null;
                }
            });
            if (pid != null && pid > 0 && isScrcpyPid(udid, pid)) {
                return pid;
            }
            TimeUnit.MILLISECONDS.sleep(300L + attempt * 100L);
        }
        throw new IOException("Failed to start scrcpy server");
    }

    /**
     * 读取设备端 PID 文件（scrcpy server 启动成功后写入）。
     */
    private Optional<Integer> readPidFile(String udid) throws IOException, InterruptedException {
        String output = shell(udid, "test -f " + PID_FILE + " && cat " + PID_FILE);
        if (output == null || output.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(output.trim()));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    /**
     * 校验 PID 是否仍为 scrcpy server。
     */
    private boolean isScrcpyPid(String udid, int pid) {
        try {
            String cmdline = readCmdline(udid, pid);
            return isScrcpyServerCmdline(cmdline);
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * 执行 adb shell 命令并返回输出文本。
     */
    private String shell(String udid, String command) throws IOException, InterruptedException {
        List<String> args = List.of(properties.getAdb().getBin(), "-s", udid, "shell", command);
        ProcessResult result = ProcessRunner.run(args, Duration.ofSeconds(8));
        return String.join("\n", result.output()).trim();
    }
}

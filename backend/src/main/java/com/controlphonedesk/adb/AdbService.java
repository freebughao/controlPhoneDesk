package com.controlphonedesk.adb;

import com.controlphonedesk.AppProperties;
import com.controlphonedesk.device.NetInterfaceInfo;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class AdbService {
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
    private static final Pattern PROP_PATTERN = Pattern.compile("\\[(.+?)]\\s*:\\s*\\[(.*?)]");

    private final String adbBin;

    public AdbService(AppProperties properties) {
        this.adbBin = properties.getAdb().getBin();
    }

    /**
     * 获取当前 adb 识别到的设备列表。
     * 输出格式来自 `adb devices -l`。
     */
    public List<AdbDevice> listDevices() throws IOException, InterruptedException {
        List<String> command = List.of(adbBin, "devices", "-l");
        ProcessResult result = ProcessRunner.run(command, DEFAULT_TIMEOUT);
        if (result.exitCode() != 0) {
            throw new IOException("adb devices failed: " + String.join("\n", result.output()));
        }
        List<AdbDevice> devices = new ArrayList<>();
        for (String line : result.output()) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("List of devices")) {
                continue;
            }
            String[] parts = line.split("\\s+");
            if (parts.length < 2) {
                continue;
            }
            AdbDevice device = new AdbDevice(parts[0], parts[1]);
            for (int i = 2; i < parts.length; i++) {
                String token = parts[i];
                int idx = token.indexOf(':');
                if (idx > 0 && idx < token.length() - 1) {
                    device.getFields().put(token.substring(0, idx), token.substring(idx + 1));
                }
            }
            devices.add(device);
        }
        return devices;
    }

    /**
     * 读取设备属性（getprop），用于型号、版本等信息展示。
     */
    public Map<String, String> getProps(String udid) throws IOException, InterruptedException {
        List<String> command = List.of(adbBin, "-s", udid, "shell", "getprop");
        ProcessResult result = ProcessRunner.run(command, DEFAULT_TIMEOUT);
        if (result.exitCode() != 0) {
            throw new IOException("adb getprop failed: " + String.join("\n", result.output()));
        }
        Map<String, String> props = new HashMap<>();
        for (String line : result.output()) {
            Matcher matcher = PROP_PATTERN.matcher(line);
            if (matcher.find()) {
                props.put(matcher.group(1), matcher.group(2));
            }
        }
        return props;
    }

    /**
     * 获取设备 IP 列表（用于展示网络接口）。
     */
    public List<NetInterfaceInfo> getInterfaces(String udid) throws IOException, InterruptedException {
        List<String> command = List.of(
            adbBin,
            "-s",
            udid,
            "shell",
            "sh",
            "-c",
            "ip -4 -f inet -o a | grep 'scope global'"
        );
        ProcessResult result = ProcessRunner.run(command, DEFAULT_TIMEOUT);
        if (result.exitCode() != 0) {
            return List.of();
        }
        List<NetInterfaceInfo> interfaces = new ArrayList<>();
        for (String line : result.output()) {
            String[] parts = line.trim().split("\\s+");
            if (parts.length < 4) {
                continue;
            }
            String name = parts[1];
            String ipWithMask = parts[3];
            String ipv4 = ipWithMask.split("/")[0];
            interfaces.add(new NetInterfaceInfo(name, ipv4));
        }
        return interfaces;
    }

    /**
     * 建立 adb 端口转发：本机 tcp:localPort -> 设备 remote。
     */
    public void forward(String udid, int localPort, String remote) throws IOException, InterruptedException {
        List<String> command = List.of(adbBin, "-s", udid, "forward", "tcp:" + localPort, remote);
        ProcessResult result = ProcessRunner.run(command, DEFAULT_TIMEOUT);
        if (result.exitCode() != 0) {
            throw new IOException("adb forward failed: " + String.join("\n", result.output()));
        }
    }

    /**
     * 清理 adb 端口转发（忽略异常，避免连接关闭失败影响流程）。
     */
    public void removeForward(String udid, int localPort) {
        try {
            List<String> command = List.of(adbBin, "-s", udid, "forward", "--remove", "tcp:" + localPort);
            ProcessRunner.run(command, DEFAULT_TIMEOUT);
        } catch (Exception ignored) {
        }
    }

    /**
     * push 本地文件到设备指定路径。
     */
    public void pushFile(String udid, String localPath, String remotePath) throws IOException, InterruptedException {
        List<String> command = List.of(adbBin, "-s", udid, "push", localPath, remotePath);
        ProcessResult result = ProcessRunner.run(command, Duration.ofSeconds(30));
        if (result.exitCode() != 0) {
            throw new IOException("adb push failed: " + String.join("\n", result.output()));
        }
    }

    /**
     * 执行 adb shell 命令（短命令）。
     */
    public void shell(String udid, String commandStr) throws IOException, InterruptedException {
        List<String> command = List.of(adbBin, "-s", udid, "shell", commandStr);
        ProcessResult result = ProcessRunner.run(command, Duration.ofSeconds(10));
        if (result.exitCode() != 0) {
            throw new IOException("adb shell failed: " + String.join("\n", result.output()));
        }
    }
}

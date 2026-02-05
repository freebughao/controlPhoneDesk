package com.controlphonedesk.adb;

import java.util.List;

public record ProcessResult(int exitCode, List<String> output) {
}

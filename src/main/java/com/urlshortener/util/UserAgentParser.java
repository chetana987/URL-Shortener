package com.urlshortener.util;

import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class UserAgentParser {

    private static final Pattern BROWSER_PATTERN = Pattern.compile(
            "(?<browser>Opera|Chrome|Firefox|Safari|Edge|MSIE|Trident|Android WebView|HeadlessChrome)[/\\s](?<version>[\\d.]+)"
    );

    private static final Pattern OS_PATTERN = Pattern.compile(
            "(?<os>Windows (?:NT|Phone)|Mac OS X|Linux|Android|iOS|iPhone|iPad)(?:[;\\s](?<osVersion>[\\d._]+))?"
    );

    private static final Pattern DEVICE_TYPE_PATTERN = Pattern.compile(
            "(?<device>Mobile|Android|iPhone|iPad|Tablet|iPod|TV|Console|Watch)"
    );

    private static final Map<String, String> BROWSER_ALIASES = Map.ofEntries(
            Map.entry("Chrome", "Chrome"),
            Map.entry("HeadlessChrome", "Headless Chrome"),
            Map.entry("Firefox", "Firefox"),
            Map.entry("Safari", "Safari"),
            Map.entry("Edge", "Microsoft Edge"),
            Map.entry("MSIE", "Internet Explorer"),
            Map.entry("Trident", "Internet Explorer"),
            Map.entry("Opera", "Opera"),
            Map.entry("Android WebView", "Android WebView")
    );

    public ParsedUserAgent parse(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return ParsedUserAgent.builder()
                    .browserName("Unknown")
                    .browserVersion("Unknown")
                    .operatingSystem("Unknown")
                    .deviceType("Desktop")
                    .deviceBrand("Unknown")
                    .build();
        }

        String browserName = parseBrowser(userAgent);
        String browserVersion = parseBrowserVersion(userAgent);
        String operatingSystem = parseOS(userAgent);
        String deviceType = parseDeviceType(userAgent);
        String deviceBrand = parseDeviceBrand(userAgent, deviceType);

        return ParsedUserAgent.builder()
                .browserName(browserName)
                .browserVersion(browserVersion)
                .operatingSystem(operatingSystem)
                .deviceType(deviceType)
                .deviceBrand(deviceBrand)
                .rawUserAgent(userAgent)
                .build();
    }

    private String parseBrowser(String userAgent) {
        Matcher matcher = BROWSER_PATTERN.matcher(userAgent);
        String browser = "Unknown";

        while (matcher.find()) {
            String matched = matcher.group("browser");
            if (BROWSER_ALIASES.containsKey(matched)) {
                browser = BROWSER_ALIASES.get(matched);
            }
        }

        if (userAgent.contains("Edg/")) {
            browser = "Microsoft Edge";
        } else if (userAgent.contains("OPR/")) {
            browser = "Opera";
        } else if (userAgent.contains("Firefox/") && !userAgent.contains("Seamonkey/")) {
            browser = "Firefox";
        }

        return browser;
    }

    private String parseBrowserVersion(String userAgent) {
        Matcher matcher = BROWSER_PATTERN.matcher(userAgent);
        String version = "Unknown";

        while (matcher.find()) {
            String matchedVersion = matcher.group("version");
            if (matchedVersion != null && !matchedVersion.isEmpty()) {
                version = matchedVersion;
            }
        }

        if (userAgent.contains("Edg/")) {
            Matcher edgMatcher = Pattern.compile("Edg/([\\d.]+)").matcher(userAgent);
            if (edgMatcher.find()) {
                version = edgMatcher.group(1);
            }
        }

        return version;
    }

    private String parseOS(String userAgent) {
        if (userAgent.contains("Windows NT 10.0")) return "Windows 10";
        if (userAgent.contains("Windows NT 6.3")) return "Windows 8.1";
        if (userAgent.contains("Windows NT 6.2")) return "Windows 8";
        if (userAgent.contains("Windows NT 6.1")) return "Windows 7";
        if (userAgent.contains("Windows NT")) return "Windows";
        if (userAgent.contains("Mac OS X")) return "macOS";
        if (userAgent.contains("Linux")) return "Linux";
        if (userAgent.contains("Android")) return "Android";
        if (userAgent.contains("iPhone") || userAgent.contains("iPad")) return "iOS";
        if (userAgent.contains("CrOS")) return "Chrome OS";

        Matcher matcher = OS_PATTERN.matcher(userAgent);
        if (matcher.find()) {
            return matcher.group("os");
        }

        return "Unknown";
    }

    private String parseDeviceType(String userAgent) {
        if (userAgent.contains("Mobile") && !userAgent.contains("Chrome Mobile")) {
            return "Mobile";
        }
        if (userAgent.contains("Android")) return "Mobile";
        if (userAgent.contains("iPhone")) return "Mobile";
        if (userAgent.contains("iPad")) return "Tablet";
        if (userAgent.contains("Tablet")) return "Tablet";
        if (userAgent.contains("TV") || userAgent.contains("tv")) return "TV";
        if (userAgent.contains("Console") || userAgent.contains("PlayStation") || userAgent.contains("Xbox")) {
            return "Console";
        }
        if (userAgent.contains("Watch")) return "Watch";
        return "Desktop";
    }

    private String parseDeviceBrand(String userAgent, String deviceType) {
        if (deviceType.equals("Mobile") || deviceType.equals("Tablet")) {
            if (userAgent.contains("Samsung")) return "Samsung";
            if (userAgent.contains("Huawei")) return "Huawei";
            if (userAgent.contains("Xiaomi")) return "Xiaomi";
            if (userAgent.contains("OnePlus")) return "OnePlus";
            if (userAgent.contains("OPPO")) return "OPPO";
            if (userAgent.contains("Vivo")) return "Vivo";
            if (userAgent.contains("iPhone")) return "Apple";
            if (userAgent.contains("iPad")) return "Apple";
        }
        return "Unknown";
    }

    @Data
    @Builder
    public static class ParsedUserAgent {
        private String browserName;
        private String browserVersion;
        private String operatingSystem;
        private String deviceType;
        private String deviceBrand;
        private String rawUserAgent;
    }
}
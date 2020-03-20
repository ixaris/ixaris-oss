/*
 * Copyright 2014 Trustin Heuiseung Lee.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ixaris.commons.protobuf.codegen;

import java.util.Locale;

public final class OsDetector {
    
    private static final String UNKNOWN = "unknown";
    
    public static String getClassifier() {
        final String osName = System.getProperty("os.name");
        final String osArch = System.getProperty("os.arch");
        
        final String detectedName = normalizeOs(osName);
        final String detectedArch = normalizeArch(osArch);
        
        return detectedName + '-' + detectedArch;
    }
    
    @SuppressWarnings({ "squid:MethodCyclomaticComplexity", "squid:S1142", "checkstyle:com.puppycrawl.tools.checkstyle.checks.metrics.NPathComplexityCheck" })
    private static String normalizeOs(final String value) {
        final String normalized = normalize(value);
        if (normalized.startsWith("aix")) {
            return "aix";
        }
        if (normalized.startsWith("hpux")) {
            return "hpux";
        }
        if (normalized.startsWith("os400")) {
            // Avoid the names such as os4000
            if (normalized.length() <= 5 || !Character.isDigit(normalized.charAt(5))) {
                return "os400";
            }
        }
        if (normalized.startsWith("linux")) {
            return "linux";
        }
        if (normalized.startsWith("macosx") || normalized.startsWith("osx")) {
            return "osx";
        }
        if (normalized.startsWith("freebsd")) {
            return "freebsd";
        }
        if (normalized.startsWith("openbsd")) {
            return "openbsd";
        }
        if (normalized.startsWith("netbsd")) {
            return "netbsd";
        }
        if (normalized.startsWith("solaris") || normalized.startsWith("sunos")) {
            return "sunos";
        }
        if (normalized.startsWith("windows")) {
            return "windows";
        }
        if (normalized.startsWith("zos")) {
            return "zos";
        }
        
        return UNKNOWN;
    }
    
    @SuppressWarnings({ "squid:MethodCyclomaticComplexity", "squid:S1142", "checkstyle:com.puppycrawl.tools.checkstyle.checks.metrics.NPathComplexityCheck" })
    private static String normalizeArch(final String value) {
        final String normalized = normalize(value);
        if (normalized.matches("^(x8664|amd64|ia32e|em64t|x64)$")) {
            return "x86_64";
        }
        if (normalized.matches("^(x8632|x86|i[3-6]86|ia32|x32)$")) {
            return "x86_32";
        }
        if (normalized.matches("^(ia64w?|itanium64)$")) {
            return "itanium_64";
        }
        if ("ia64n".equals(normalized)) {
            return "itanium_32";
        }
        if (normalized.matches("^(sparc|sparc32)$")) {
            return "sparc_32";
        }
        if (normalized.matches("^(sparcv9|sparc64)$")) {
            return "sparc_64";
        }
        if (normalized.matches("^(arm|arm32)$")) {
            return "arm_32";
        }
        if ("aarch64".equals(normalized)) {
            return "aarch_64";
        }
        if (normalized.matches("^(mips|mips32)$")) {
            return "mips_32";
        }
        if (normalized.matches("^(mipsel|mips32el)$")) {
            return "mipsel_32";
        }
        if ("mips64".equals(normalized)) {
            return "mips_64";
        }
        if ("mips64el".equals(normalized)) {
            return "mipsel_64";
        }
        if (normalized.matches("^(ppc|ppc32)$")) {
            return "ppc_32";
        }
        if (normalized.matches("^(ppcle|ppc32le)$")) {
            return "ppcle_32";
        }
        if ("ppc64".equals(normalized)) {
            return "ppc_64";
        }
        if ("ppc64le".equals(normalized)) {
            return "ppcle_64";
        }
        if ("s390".equals(normalized)) {
            return "s390_32";
        }
        if ("s390x".equals(normalized)) {
            return "s390_64";
        }
        
        return UNKNOWN;
    }
    
    private static String normalize(final String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "");
    }
    
    private OsDetector() {}
    
}

# SPDX-License-Identifier: GPL-3.0-or-later
# Rules recommended by libxposed: keep the module entry class and rewrite java_init.list.
-dontwarn io.github.libxposed.annotation.**
-adaptresourcefilecontents META-INF/xposed/java_init.list
-keep,allowoptimization,allowobfuscation public class * extends io.github.libxposed.api.XposedModule {
    public <init>();
}

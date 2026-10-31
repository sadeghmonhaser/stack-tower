@Suppress("UNCHECKED_CAST")
fun removeEnvVar(name: String) {
    try {
        val processEnvironment = Class.forName("java.lang.ProcessEnvironment")
        val fields = arrayOf("theEnvironment", "theCaseInsensitiveEnvironment", "theUnmodifiableEnvironment")
        for (fieldName in fields) {
            try {
                val field = processEnvironment.getDeclaredField(fieldName)
                field.isAccessible = true
                val map = field.get(null) as? MutableMap<Any, Any>
                map?.keys?.removeIf { key ->
                    val keyStr = if (key is String) key else key.toString()
                    keyStr.equals(name, ignoreCase = true)
                }
            } catch (_: Exception) {}
        }
    } catch (_: Exception) {}
}
removeEnvVar("ANDROID_PREFS_ROOT")

pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositories { google(); mavenCentral() }
}
rootProject.name = "StackTower"
include(":app")

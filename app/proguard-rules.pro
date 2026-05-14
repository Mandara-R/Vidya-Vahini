# Add project-specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\suman\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.kts.

# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# Firebase specific rules
-keep class com.google.firebase.** { *; }
-keep class com.vidyavahini.app.data.model.** { *; }

# Navigation component
-keep class androidx.navigation.** { *; }

# Lifecycle
-keep class androidx.lifecycle.** { *; }

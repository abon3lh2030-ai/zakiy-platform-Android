# قواعد أساسية - المشروع ما يفعّل التصغير (minifyEnabled) بالنسخة الحالية،
# هذا الملف جاهز لو فُعّل لاحقًا
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.zakiy.platform.network.dto.** { *; }
-dontwarn org.webrtc.**

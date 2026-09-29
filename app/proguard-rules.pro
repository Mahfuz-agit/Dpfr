# Dpfr release rules.
# Classes referenced from AndroidManifest.xml are kept automatically by R8.
-keepattributes *Annotation*,Signature,InnerClasses
-keep class com.dpfr.app.overlay.views.** { *; }

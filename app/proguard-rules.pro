# Room
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep data models used for backup JSON serialization
-keep class com.riad.bizaccount.data.local.entity.** { *; }

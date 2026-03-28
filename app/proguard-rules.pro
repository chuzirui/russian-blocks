# Suppress R8 warnings
-dontwarn android.media.LoudnessCodecController$OnLoudnessCodecUpdateListener
-dontwarn android.media.LoudnessCodecController

# Keep game classes
-keep class com.russianblocks.game.** { *; }

# Google AdMob / Play Services
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }

# Google Play Billing
-keep class com.android.billingclient.** { *; }
-keep class com.android.vending.billing.** { *; }

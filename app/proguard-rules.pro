# Ledger ProGuard / R8 keep rules
#
# No custom keep rules needed:
# - Android components (Activity, Service, Receiver): handled by AAPT2
# - Room database & DAOs: consumer rules in androidx.room
# - Retrofit interfaces: consumer rules in retrofit2 (2.9.0+)
# - Kotlin coroutines: consumer rules in kotlinx-coroutines-core
# - Gson serialization: handled by @SerializedName on all data classes
#
# Default optimization file: proguard-android-optimize.txt

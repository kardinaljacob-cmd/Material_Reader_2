# R8 / ProGuard rules for Листай (com.aistudio.listay.krmxvp / com.example)

# Keep readable line numbers while obfuscating source file names
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations
-renamesourcefileattribute SourceFile

# Entry points & BuildConfig
-keep class com.example.MainActivity { *; }
-keep class com.example.BuildConfig { *; }

# ViewModels instantiated via reflection by viewModel() / AndroidViewModelFactory
-keep class com.example.ui.viewmodel.** { *; }
-keepclassmembers class * extends androidx.lifecycle.AndroidViewModel {
    public <init>(android.app.Application);
}
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
}

# Data models, entities, and parsed book structures
-keep class com.example.data.model.** { *; }
-keep class com.example.data.prefs.** { *; }
-keep class com.example.ui.reader.** { *; }

# File format parsers & EPUB CSS style engine (EPUB, FB2, PDF, TXT, HTML, MD)
-keep class com.example.data.parser.** { *; }
-keep class com.example.data.BookRepository { *; }
-keep class com.example.data.BundledLibrarySeeder { *; }

# Fonts, themes, and custom typography enums / families
-keep class com.example.ui.theme.** { *; }
-keep class com.example.R$font { *; }
-keep class com.example.R$drawable { *; }

# Preserve all enums and enum valueOf/values methods (used by Room & DataStore persistence)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keep enum com.example.data.model.** { *; }
-keep enum com.example.ui.theme.** { *; }
-keep enum com.example.ui.viewmodel.LibraryFilter { *; }
-keep enum com.example.ui.components.MainNavTab { *; }

# Room database & KSP-generated *_Impl classes
-keep class com.example.data.db.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.RoomDatabase$Callback { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * {
    @androidx.room.TypeConverter <methods>;
}

# Jetpack DataStore Preferences (protobuf-javalite reflection)
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}
-keep class androidx.datastore.preferences.** { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Coil image loading
-keep class coil.** { *; }
-dontwarn coil.**

# Navigation & SavedState
-keepclassmembers class * extends androidx.lifecycle.SavedStateHandle { *; }
-keepnames class * implements android.os.Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

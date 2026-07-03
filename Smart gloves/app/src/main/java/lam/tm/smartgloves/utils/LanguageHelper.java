package lam.tm.smartgloves.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import java.util.Locale;

public class LanguageHelper {
    private static final String PREF_NAME = "language_prefs";
    private static final String KEY_LANGUAGE = "language";
    
    // Language codes
    public static final String LANGUAGE_VI = "vi";
    public static final String LANGUAGE_EN = "en";
    
    /**
     * Lấy ngôn ngữ hiện tại từ SharedPreferences
     */
    public static String getLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LANGUAGE, LANGUAGE_VI); // Default là tiếng Việt
    }
    
    /**
     * Lưu ngôn ngữ vào SharedPreferences
     */
    public static void setLanguage(Context context, String language) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANGUAGE, language).apply();
    }
    
    /**
     * Áp dụng ngôn ngữ cho context
     */
    public static Context setLocale(Context context, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        
        Configuration configuration = context.getResources().getConfiguration();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale);
            return context.createConfigurationContext(configuration);
        } else {
            configuration.locale = locale;
            Resources resources = context.getResources();
            resources.updateConfiguration(configuration, resources.getDisplayMetrics());
            return context;
        }
    }
    
    /**
     * Khởi tạo ngôn ngữ khi app khởi động
     */
    public static Context initLanguage(Context context) {
        String savedLanguage = getLanguage(context);
        return setLocale(context, savedLanguage);
    }
    
    /**
     * Chuyển đổi giữa tiếng Việt và tiếng Anh
     */
    public static String toggleLanguage(Context context) {
        String currentLanguage = getLanguage(context);
        String newLanguage = currentLanguage.equals(LANGUAGE_VI) ? LANGUAGE_EN : LANGUAGE_VI;
        setLanguage(context, newLanguage);
        return newLanguage;
    }
    
    /**
     * Kiểm tra xem đang ở ngôn ngữ nào
     */
    public static boolean isVietnamese(Context context) {
        return getLanguage(context).equals(LANGUAGE_VI);
    }
}


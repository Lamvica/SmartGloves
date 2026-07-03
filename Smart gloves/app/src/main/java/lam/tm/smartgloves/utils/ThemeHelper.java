package lam.tm.smartgloves.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class ThemeHelper {
    private static final String PREF_NAME = "theme_prefs";
    private static final String KEY_THEME_MODE = "theme_mode";
    
    // Theme modes
    public static final int MODE_LIGHT = AppCompatDelegate.MODE_NIGHT_NO;
    public static final int MODE_DARK = AppCompatDelegate.MODE_NIGHT_YES;
    public static final int MODE_SYSTEM = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    
    /**
     * Lấy theme mode hiện tại từ SharedPreferences
     */
    public static int getThemeMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_THEME_MODE, MODE_SYSTEM);
    }
    
    /**
     * Lưu theme mode vào SharedPreferences
     */
    public static void setThemeMode(Context context, int mode) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply();
    }
    
    /**
     * Áp dụng theme mode
     */
    public static void applyTheme(int mode) {
        AppCompatDelegate.setDefaultNightMode(mode);
    }
    
    /**
     * Khởi tạo theme khi app khởi động
     */
    public static void initTheme(Context context) {
        int savedMode = getThemeMode(context);
        applyTheme(savedMode);
    }
    
    /**
     * Chuyển đổi giữa light và dark mode
     */
    public static int toggleTheme(Context context) {
        int currentMode = getThemeMode(context);
        int newMode;
        
        if (currentMode == MODE_LIGHT) {
            newMode = MODE_DARK;
        } else {
            newMode = MODE_LIGHT;
        }
        
        setThemeMode(context, newMode);
        applyTheme(newMode);
        return newMode;
    }
    
    /**
     * Kiểm tra xem đang ở dark mode hay không
     */
    public static boolean isDarkMode(Context context) {
        int currentMode = getThemeMode(context);
        if (currentMode == MODE_SYSTEM) {
            // Nếu là system mode, kiểm tra system setting
            int systemMode = context.getResources().getConfiguration().uiMode & 
                           android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            return systemMode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        }
        return currentMode == MODE_DARK;
    }
}


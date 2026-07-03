package lam.tm.smartgloves;

import android.content.Context;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import lam.tm.smartgloves.databinding.ActivityMainBinding;
import lam.tm.smartgloves.utils.LanguageHelper;
import lam.tm.smartgloves.utils.ThemeHelper;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void attachBaseContext(Context newBase) {
        // Áp dụng ngôn ngữ trước khi attach base context
        Context context = LanguageHelper.initLanguage(newBase);
        super.attachBaseContext(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Khởi tạo theme trước khi setContentView
        ThemeHelper.initTheme(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Bottom navigation đã bị ẩn, không cần setup nữa
        // Tất cả navigation được thực hiện thông qua grid menu trong HomeFragment
    }

}
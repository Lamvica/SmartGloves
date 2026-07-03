package lam.tm.smartgloves.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import lam.tm.smartgloves.MainActivity;
import lam.tm.smartgloves.R;

public class LoginActivity extends AppCompatActivity {

    private static final String VALID_USERNAME = "Lamvica";
    private static final String VALID_PASSWORD = "123";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        EditText editUsername = findViewById(R.id.edit_username);
        EditText editPassword = findViewById(R.id.edit_password);
        Button buttonLogin = findViewById(R.id.button_login);

        buttonLogin.setOnClickListener(v -> {
            String username = editUsername.getText() != null
                    ? editUsername.getText().toString().trim()
                    : "";
            String password = editPassword.getText() != null
                    ? editPassword.getText().toString()
                    : "";

            if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ tài khoản và mật khẩu", Toast.LENGTH_SHORT).show();
                return;
            }

            if (VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password)) {
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Tài khoản hoặc mật khẩu không đúng", Toast.LENGTH_SHORT).show();
            }
        });
    }
}


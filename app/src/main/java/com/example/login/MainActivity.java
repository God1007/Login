package com.example.login;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.login.data.User;
import com.example.login.data.UserDao;
import com.example.login.data.UserDatabase;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "login_prefs";
    private static final String KEY_LAST_USER = "last_user";
    private static final String KEY_LAST_LOGIN_TIME = "last_login_time";
    private static final String KEY_REMEMBER = "remember";

    private TextInputLayout usernameLayout;
    private TextInputLayout passwordLayout;
    private TextInputEditText etUsername;
    private TextInputEditText etPassword;
    private CheckBox cbRemember;
    private TextView tvStorageSummary;
    private TextView persistenceInfo;

    private SharedPreferences preferences;
    private UserDao userDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        userDao = UserDatabase.getInstance(this).userDao();

        setupViews();
        restoreFromPreferences();
        refreshStorageSummary();
    }

    private void setupViews() {
        usernameLayout = findViewById(R.id.usernameLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        cbRemember = findViewById(R.id.cbRemember);
        tvStorageSummary = findViewById(R.id.tvStorageSummary);
        persistenceInfo = findViewById(R.id.persistenceInfo);

        MaterialButton btnLogin = findViewById(R.id.btnLogin);
        MaterialButton btnRegister = findViewById(R.id.btnRegister);
        MaterialButton btnClearPrefs = findViewById(R.id.btnClearPrefs);

        btnLogin.setOnClickListener(v -> handleLogin());
        btnRegister.setOnClickListener(v -> handleRegister());
        btnClearPrefs.setOnClickListener(v -> clearPreferences());
    }

    private void handleLogin() {
        clearErrors();
        final String username = getText(etUsername);
        final String password = getText(etPassword);

        if (!isInputValid(username, password)) {
            return;
        }

        executor.execute(() -> {
            User user = userDao.findByUsername(username);
            runOnUiThread(() -> {
                if (user != null && password.equals(user.getPassword())) {
                    savePreferences(username, cbRemember.isChecked());
                    Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show();
                    refreshStorageSummary();
                } else {
                    Toast.makeText(this, R.string.login_failed, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void handleRegister() {
        clearErrors();
        final String username = getText(etUsername);
        final String password = getText(etPassword);

        if (!isInputValid(username, password)) {
            return;
        }

        executor.execute(() -> {
            User existing = userDao.findByUsername(username);
            if (existing != null) {
                runOnUiThread(() -> {
                    etPassword.requestFocus();
                    Toast.makeText(this, R.string.register_duplicate, Toast.LENGTH_SHORT).show();
                });
                return;
            }

            userDao.insert(new User(username, password));
            runOnUiThread(() -> {
                savePreferences(username, true);
                cbRemember.setChecked(true);
                Toast.makeText(this, R.string.register_success, Toast.LENGTH_SHORT).show();
                refreshStorageSummary();
            });
        });
    }

    private void clearPreferences() {
        preferences.edit().clear().apply();
        cbRemember.setChecked(false);
        persistenceInfo.setText(R.string.first_login_hint);
        Toast.makeText(this, R.string.prefs_cleared, Toast.LENGTH_SHORT).show();
        refreshStorageSummary();
    }

    private void restoreFromPreferences() {
        boolean remember = preferences.getBoolean(KEY_REMEMBER, false);
        cbRemember.setChecked(remember);
        if (remember) {
            String lastUser = preferences.getString(KEY_LAST_USER, "");
            etUsername.setText(lastUser);
            String lastLogin = preferences.getString(KEY_LAST_LOGIN_TIME, getString(R.string.first_login_hint));
            persistenceInfo.setText(getString(R.string.welcome_back, lastUser, lastLogin));
        } else {
            persistenceInfo.setText(R.string.first_login_hint);
        }
    }

    private void savePreferences(String username, boolean remember) {
        SharedPreferences.Editor editor = preferences.edit();
        if (remember) {
            String timestamp = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
                    .format(new Date());
            editor.putString(KEY_LAST_USER, username);
            editor.putString(KEY_LAST_LOGIN_TIME, timestamp);
            editor.putBoolean(KEY_REMEMBER, true);
            editor.apply();
            persistenceInfo.setText(getString(R.string.welcome_back, username, timestamp));
        } else {
            editor.clear().apply();
            persistenceInfo.setText(R.string.first_login_hint);
        }
    }

    private void refreshStorageSummary() {
        executor.execute(() -> {
            int userCount = userDao.getUserCount();
            List<String> usernames = userDao.getAllUsernames();
            boolean remember = preferences.getBoolean(KEY_REMEMBER, false);
            String lastUser = preferences.getString(KEY_LAST_USER, "");
            String lastTime = preferences.getString(KEY_LAST_LOGIN_TIME, getString(R.string.first_login_hint));

            StringBuilder builder = new StringBuilder();
            builder.append(getString(R.string.storage_summary)).append("\n\n")
                    .append("Room 中保存的账户数量：").append(userCount);
            if (!usernames.isEmpty()) {
                builder.append("\n最近的账户：").append(TextUtils.join(", ", usernames));
            }
            builder.append("\nSharedPreferences 记住我：").append(remember ? "已开启" : "未开启");
            if (remember) {
                builder.append("\n最近登录用户：").append(lastUser)
                        .append("（").append(lastTime).append("）");
            }

            runOnUiThread(() -> tvStorageSummary.setText(builder.toString()));
        });
    }

    private boolean isInputValid(String username, String password) {
        boolean valid = true;
        if (TextUtils.isEmpty(username)) {
            usernameLayout.setError(getString(R.string.fields_required));
            valid = false;
        }
        if (TextUtils.isEmpty(password) || password.length() < 4) {
            passwordLayout.setError(getString(R.string.fields_required));
            valid = false;
        }
        return valid;
    }

    private void clearErrors() {
        usernameLayout.setError(null);
        passwordLayout.setError(null);
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}

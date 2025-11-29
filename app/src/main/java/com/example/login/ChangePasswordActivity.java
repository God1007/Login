package com.example.login;

import android.os.Bundle;
import android.text.TextUtils;
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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChangePasswordActivity extends AppCompatActivity {

    private TextInputLayout usernameLayout;
    private TextInputLayout currentPasswordLayout;
    private TextInputLayout newPasswordLayout;
    private TextInputEditText etUsername;
    private TextInputEditText etCurrentPassword;
    private TextInputEditText etNewPassword;

    private UserDao userDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_change_password);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.changePasswordRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.action_change_password);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        userDao = UserDatabase.getInstance(this).userDao();
        setupViews();
    }

    private void setupViews() {
        usernameLayout = findViewById(R.id.usernameLayout);
        currentPasswordLayout = findViewById(R.id.currentPasswordLayout);
        newPasswordLayout = findViewById(R.id.newPasswordLayout);
        etUsername = findViewById(R.id.etUsername);
        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewPassword = findViewById(R.id.etNewPassword);

        MaterialButton btnConfirmChange = findViewById(R.id.btnConfirmChange);
        btnConfirmChange.setOnClickListener(v -> handleChangePassword());
    }

    private void handleChangePassword() {
        clearErrors();
        final String username = getText(etUsername);
        final String currentPassword = getText(etCurrentPassword);
        final String newPassword = getText(etNewPassword);

        if (!isChangePasswordValid(username, currentPassword, newPassword)) {
            return;
        }

        executor.execute(() -> {
            User user = userDao.findByUsername(username);
            if (user == null) {
                runOnUiThread(() -> Toast.makeText(this, R.string.user_not_found, Toast.LENGTH_SHORT).show());
                return;
            }
            if (!currentPassword.equals(user.getPassword())) {
                runOnUiThread(() -> Toast.makeText(this, R.string.change_password_invalid_current, Toast.LENGTH_SHORT).show());
                return;
            }

            userDao.updatePassword(username, newPassword);
            runOnUiThread(() -> {
                Toast.makeText(this, R.string.change_password_success, Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }

    private boolean isChangePasswordValid(String username, String currentPassword, String newPassword) {
        boolean valid = true;
        if (TextUtils.isEmpty(username)) {
            usernameLayout.setError(getString(R.string.fields_required));
            valid = false;
        }
        if (TextUtils.isEmpty(currentPassword)) {
            currentPasswordLayout.setError(getString(R.string.fields_required));
            valid = false;
        }
        if (TextUtils.isEmpty(newPassword) || newPassword.length() < 4) {
            newPasswordLayout.setError(getString(R.string.fields_required));
            valid = false;
        }
        return valid;
    }

    private void clearErrors() {
        usernameLayout.setError(null);
        currentPasswordLayout.setError(null);
        newPasswordLayout.setError(null);
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}

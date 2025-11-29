package com.example.login;

import android.os.Bundle;
import android.text.TextUtils;
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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputLayout usernameLayout;
    private TextInputEditText etUsername;
    private TextView resetResult;

    private UserDao userDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.forgotPasswordRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.action_forgot_password);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        userDao = UserDatabase.getInstance(this).userDao();
        setupViews();
    }

    private void setupViews() {
        usernameLayout = findViewById(R.id.usernameLayout);
        etUsername = findViewById(R.id.etUsername);
        resetResult = findViewById(R.id.resetResult);

        MaterialButton btnRequestReset = findViewById(R.id.btnRequestReset);
        btnRequestReset.setOnClickListener(v -> handleForgotPassword());
    }

    private void handleForgotPassword() {
        clearErrors();
        final String username = getText(etUsername);

        if (TextUtils.isEmpty(username)) {
            usernameLayout.setError(getString(R.string.fields_required));
            return;
        }

        executor.execute(() -> {
            User user = userDao.findByUsername(username);
            if (user == null) {
                runOnUiThread(() -> Toast.makeText(this, R.string.user_not_found, Toast.LENGTH_SHORT).show());
                return;
            }

            String temporaryPassword = generateTemporaryPassword();
            userDao.updatePassword(username, temporaryPassword);
            runOnUiThread(() -> {
                resetResult.setText(getString(R.string.forgot_password_success, temporaryPassword));
                Toast.makeText(this, R.string.forgot_password_generated, Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void clearErrors() {
        usernameLayout.setError(null);
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private String generateTemporaryPassword() {
        int random = 100000 + (int) (Math.random() * 900000);
        return String.valueOf(random);
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

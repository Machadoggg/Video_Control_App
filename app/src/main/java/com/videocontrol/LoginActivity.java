package com.videocontrol;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    // ── Contraseña quemada ─────────────────────────────────────────────────
    private static final String APP_PASSWORD = "barracuda";
    private static final String PREFS_NAME   = "barracuda_prefs";
    private static final String KEY_LOGGED   = "is_logged_in";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Si ya estaba logueado, ir directo a MainActivity
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (prefs.getBoolean(KEY_LOGGED, false)) {
            goToMain();
            return;
        }

        setContentView(R.layout.activity_login);

        EditText passwordInput = findViewById(R.id.passwordInput);
        Button   loginButton   = findViewById(R.id.loginButton);
        TextView errorText     = findViewById(R.id.errorText);

        loginButton.setOnClickListener(v -> {
            String entered = passwordInput.getText().toString().trim();

            if (entered.equals(APP_PASSWORD)) {
                // Guardar sesion
                prefs.edit().putBoolean(KEY_LOGGED, true).apply();
                errorText.setVisibility(View.GONE);
                goToMain();
            } else {
                errorText.setVisibility(View.VISIBLE);
                passwordInput.setText("");
                passwordInput.requestFocus();
                // Vibrar el campo de error
                passwordInput.animate()
                        .translationX(-16f).setDuration(60)
                        .withEndAction(() -> passwordInput.animate()
                                .translationX(16f).setDuration(60)
                                .withEndAction(() -> passwordInput.animate()
                                        .translationX(0f).setDuration(60).start())
                                .start())
                        .start();
            }
        });

        // Enter en el campo → intenta login
        passwordInput.setOnEditorActionListener((v, actionId, event) -> {
            loginButton.performClick();
            return true;
        });
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}


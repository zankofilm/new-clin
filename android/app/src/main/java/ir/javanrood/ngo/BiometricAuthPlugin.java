package ir.javanrood.ngo;

import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.util.concurrent.Executor;

@CapacitorPlugin(name = "BiometricAuth")
public class BiometricAuthPlugin extends Plugin {

    private static final int AUTHENTICATORS =
        BiometricManager.Authenticators.BIOMETRIC_STRONG |
        BiometricManager.Authenticators.DEVICE_CREDENTIAL;

    @PluginMethod
    public void isAvailable(PluginCall call) {
        int status = BiometricManager.from(getContext()).canAuthenticate(AUTHENTICATORS);
        JSObject result = new JSObject();
        result.put("available", status == BiometricManager.BIOMETRIC_SUCCESS);
        result.put("status", status);
        call.resolve(result);
    }

    @PluginMethod
    public void authenticate(PluginCall call) {
        if (!(getActivity() instanceof FragmentActivity)) {
            call.reject("Biometric authentication is unavailable in this activity.");
            return;
        }

        int status = BiometricManager.from(getContext()).canAuthenticate(AUTHENTICATORS);
        if (status != BiometricManager.BIOMETRIC_SUCCESS) {
            call.reject("اثر انگشت یا قفل امن دستگاه در دسترس نیست.", String.valueOf(status));
            return;
        }

        FragmentActivity activity = (FragmentActivity) getActivity();
        activity.runOnUiThread(() -> {
            Executor executor = ContextCompat.getMainExecutor(activity);
            BiometricPrompt prompt = new BiometricPrompt(activity, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        call.reject(errString != null ? errString.toString() : "تأیید هویت انجام نشد.", String.valueOf(errorCode));
                    }

                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        JSObject response = new JSObject();
                        response.put("authenticated", true);
                        call.resolve(response);
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                    }
                });

            String title = call.getString("title", "تأیید هویت");
            String subtitle = call.getString("subtitle", "برای ادامه هویت خود را تأیید کنید.");

            BiometricPrompt.PromptInfo.Builder builder = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setAllowedAuthenticators(AUTHENTICATORS)
                .setConfirmationRequired(false);

            if (subtitle != null && !subtitle.trim().isEmpty()) {
                builder.setSubtitle(subtitle);
            }

            prompt.authenticate(builder.build());
        });
    }
}

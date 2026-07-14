package ir.javanrood.ngo;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CapacitorPlugin(name = "FileSaver")
public class FileSaverPlugin extends Plugin {

    private static final class SaveSession {
        final OutputStream output;
        final Uri uri;
        final File file;
        final String location;

        SaveSession(OutputStream output, Uri uri, File file, String location) {
            this.output = output;
            this.uri = uri;
            this.file = file;
            this.location = location;
        }
    }

    private final Map<String, SaveSession> sessions = new ConcurrentHashMap<>();

    @PluginMethod
    public void beginFile(PluginCall call) {
        String rawName = call.getString("fileName", "export.bin");
        String mimeType = call.getString("mimeType", "application/octet-stream");
        String fileName = sanitizeFileName(rawName);

        try {
            SaveSession session = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? createMediaStoreSession(fileName, mimeType)
                : createAppDownloadSession(fileName);
            String token = UUID.randomUUID().toString();
            sessions.put(token, session);

            JSObject result = new JSObject();
            result.put("token", token);
            result.put("location", session.location);
            call.resolve(result);
        } catch (Exception error) {
            call.reject("آماده‌سازی محل ذخیره فایل انجام نشد: " + error.getMessage(), error);
        }
    }

    @PluginMethod
    public void appendFileChunk(PluginCall call) {
        String token = call.getString("token");
        String base64 = call.getString("base64");
        SaveSession session = token == null ? null : sessions.get(token);

        if (session == null) {
            call.reject("نشست ذخیره فایل معتبر نیست.");
            return;
        }
        if (base64 == null || base64.isEmpty()) {
            call.reject("بخش ارسالی فایل خالی است.");
            return;
        }

        try {
            byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
            session.output.write(bytes);
            JSObject result = new JSObject();
            result.put("written", bytes.length);
            call.resolve(result);
        } catch (Exception error) {
            abortSession(token, session);
            call.reject("نوشتن فایل انجام نشد: " + error.getMessage(), error);
        }
    }

    @PluginMethod
    public void finishFile(PluginCall call) {
        String token = call.getString("token");
        SaveSession session = token == null ? null : sessions.remove(token);

        if (session == null) {
            call.reject("نشست ذخیره فایل معتبر نیست.");
            return;
        }

        try {
            session.output.flush();
            session.output.close();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && session.uri != null) {
                ContentValues completed = new ContentValues();
                completed.put(MediaStore.Downloads.IS_PENDING, 0);
                getContext().getContentResolver().update(session.uri, completed, null, null);
            }

            JSObject result = new JSObject();
            result.put("saved", true);
            result.put("location", session.location);
            if (session.uri != null) result.put("uri", session.uri.toString());
            if (session.file != null) result.put("path", session.file.getAbsolutePath());
            call.resolve(result);
        } catch (Exception error) {
            cleanupSession(session);
            call.reject("نهایی‌سازی فایل انجام نشد: " + error.getMessage(), error);
        }
    }

    @PluginMethod
    public void abortFile(PluginCall call) {
        String token = call.getString("token");
        SaveSession session = token == null ? null : sessions.remove(token);
        if (session != null) cleanupSession(session);
        call.resolve();
    }

    private SaveSession createMediaStoreSession(String fileName, String mimeType) throws Exception {
        Context context = getContext();
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
        values.put(MediaStore.Downloads.MIME_TYPE, mimeType);
        values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/SamaneSamanha");
        values.put(MediaStore.Downloads.IS_PENDING, 1);

        Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri == null) {
            throw new IllegalStateException("Download destination could not be created.");
        }

        OutputStream output = resolver.openOutputStream(uri);
        if (output == null) {
            resolver.delete(uri, null, null);
            throw new IllegalStateException("Download stream could not be opened.");
        }

        return new SaveSession(output, uri, null, "Downloads/SamaneSamanha");
    }

    private SaveSession createAppDownloadSession(String fileName) throws Exception {
        File base = getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (base == null) base = getContext().getFilesDir();

        File folder = new File(base, "SamaneSamanha");
        if (!folder.exists() && !folder.mkdirs()) {
            throw new IllegalStateException("Download folder could not be created.");
        }

        File outputFile = uniqueFile(folder, fileName);
        return new SaveSession(
            new FileOutputStream(outputFile),
            null,
            outputFile,
            outputFile.getAbsolutePath()
        );
    }

    private void abortSession(String token, SaveSession session) {
        sessions.remove(token);
        cleanupSession(session);
    }

    private void cleanupSession(SaveSession session) {
        try {
            session.output.close();
        } catch (Exception ignored) {}

        if (session.uri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                getContext().getContentResolver().delete(session.uri, null, null);
            } catch (Exception ignored) {}
        }

        if (session.file != null && session.file.exists()) {
            try {
                //noinspection ResultOfMethodCallIgnored
                session.file.delete();
            } catch (Exception ignored) {}
        }
    }

    private File uniqueFile(File folder, String fileName) {
        File candidate = new File(folder, fileName);
        if (!candidate.exists()) return candidate;

        int dot = fileName.lastIndexOf('.');
        String stem = dot > 0 ? fileName.substring(0, dot) : fileName;
        String extension = dot > 0 ? fileName.substring(dot) : "";
        int index = 2;
        while (candidate.exists()) {
            candidate = new File(folder, stem + " (" + index + ")" + extension);
            index++;
        }
        return candidate;
    }

    private String sanitizeFileName(String value) {
        String cleaned = value == null ? "export.bin" : value.trim();
        cleaned = cleaned.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_");
        if (cleaned.isEmpty()) cleaned = "export.bin";
        return cleaned;
    }

    @Override
    protected void handleOnDestroy() {
        for (SaveSession session : sessions.values()) {
            cleanupSession(session);
        }
        sessions.clear();
        super.handleOnDestroy();
    }
}

# امضای ثابت APK در GitHub

خروجی Debug قابل نصب است، اما برای نصب نسخه‌های بعدی روی نسخه قبلی باید همه نسخه‌ها با یک کلید ثابت امضا شوند.

چهار Secret زیر را در مسیر **Repository → Settings → Secrets and variables → Actions** بسازید:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

برای ساخت کلید روی رایانه‌ای که Java دارد:

```bash
keytool -genkeypair -v \
  -keystore samane-samanha-release.jks \
  -alias samane-samanha \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000
```

تبدیل فایل کلید به Base64:

### Linux

```bash
base64 -w 0 samane-samanha-release.jks
```

### macOS

```bash
base64 < samane-samanha-release.jks | tr -d '\n'
```

### PowerShell

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("samane-samanha-release.jks"))
```

رشته خروجی را داخل `ANDROID_KEYSTORE_BASE64` قرار دهید. فایل اصلی JKS و رمزهای آن را در جای امن نگه دارید؛ گم‌شدن کلید به معنی ناتوانی در انتشار آپدیت سازگار است.

# سامانه مدیریت سمن‌ها — نسخه Android

این مخزن، فایل HTML سامانه را با Capacitor 8 داخل یک برنامه بومی Android اجرا می‌کند.

## امکانات نسخه Android

- اجرای آفلاین فایل‌های سامانه
- نگهداری اطلاعات در LocalStorage و IndexedDB
- انتخاب CSV، Excel، JSON، ZIP، تصویر و PDF از گوشی
- ورود با اثر انگشت یا قفل امن دستگاه
- ذخیره خروجی‌های ZIP، CSV و گزارش‌ها در `Downloads/SamaneSamanha`
- پشتیبانی راست‌به‌چپ و نمایش تمام‌صفحه موبایل
- ساخت خودکار APK در GitHub Actions

## ساخت APK در GitHub

1. تمام فایل‌های این پوشه را در یک Repository جدید GitHub بارگذاری کنید.
2. وارد زبانه **Actions** شوید.
3. Workflow با نام **Build Android APK** را باز کنید.
4. روی **Run workflow** بزنید.
5. پس از سبز شدن Workflow، از پایین همان صفحه Artifact با نام `Samane-Samanha-Android` را دانلود کنید.
6. فایل ZIP را باز و APK را روی گوشی نصب کنید.

خروجی بدون Secret امضا، از نوع Debug است و مستقیماً نصب می‌شود. برای نسخه نهایی با امضای ثابت، راهنمای `docs/SIGNING_FA.md` را اجرا کنید.

## انتشار مستقیم در Releases

یک Tag مانند زیر بسازید و Push کنید:

```bash
git tag v1.0.0
git push origin v1.0.0
```

Workflow به‌صورت خودکار APK را در بخش **Releases** همان Repository قرار می‌دهد.

## مشخصات فعلی

- نام برنامه: `سامانه سمن‌ها`
- Application ID: `ir.javanrood.ngo`
- نسخه: `1.0.0`
- حداقل Android: 7.0، API 24
- Target SDK: Android 16، API 36
- Node.js: 22
- Java: 21

## تغییر فایل اصلی سامانه

فایل‌های وب داخل پوشه `www` هستند. پس از تغییر آن‌ها اجرا کنید:

```bash
npm install
npx cap sync android
```

## آیکون

در حال حاضر آیکون پیش‌فرض قرار دارد. آیکون نهایی باید مربع، PNG و ترجیحاً ۱۰۲۴×۱۰۲۴ پیکسل باشد. پس از جایگزینی، تمام اندازه‌های لازم Android باید در پوشه‌های `mipmap-*` تولید شوند.

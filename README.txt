بناء ملف APK لتطبيق مكتب القرقاري

قبل البناء: ضع صورة المحامي IMG-20261012-WA0000.jpg داخل مجلد www بجانب index.html (اختياري).

الطريقة 1 — بدون برامج (GitHub):
1) أنشئ مستودعًا خاصًا (Private) على github.com وارفع كل محتويات هذا المجلد (بما فيها المجلد المخفي .github).
2) من تبويب Actions اختر "Build APK" ثم Run workflow.
3) بعد دقائق افتح التشغيل ونزّل الملف office-app-apk (بداخله app-debug.apk) وثبّته على الموبايل.

الطريقة 2 — على الكمبيوتر (Node 18+ و Android Studio):
  npm install
  npx cap add android
  npx cap sync android
  npx cap open android
ثم من Android Studio: Build > Build APK(s).

بعد التثبيت: كلمة المرور الافتراضية 1234 (تتغير من الإعدادات).

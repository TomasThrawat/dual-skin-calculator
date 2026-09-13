# Dual Skin Calculator (Kotlin / Android)

آلة حاسبة أندرويد بلغة Kotlin، فيها **زرار واحد فوق الشاشة (🔁 تبديل الواجهة)**
بيبدّل بين وضعين، ونفس الزرار بيرجّع للوضع التاني تاني:

1. **Minimal** — واجهة بسيطة (آلة حاسبة عادية: أرقام + عمليات أساسية).
2. **Scientific** — نفس شكل وألوان الآلة الحاسبة اللي في الصورة المرفقة
   (خلفية غامقة، SHIFT برتقالي، ALPHA بنفسجي، شاشة عرض فاتحة، نفس ترتيب
   الصفوف تقريبًا: SHIFT/ALPHA/MODE، Sin/Cos/Tan، RCL/ENG، لوحة الأرقام...).

معمول بـ Views عادية (بدون Jetpack Compose) عشان يفضل بسيط وسهل التعديل.

## إيه اللي شغّال فعليًا (Engine مكتوب يدويًا، من غير مكتبة خارجية)

`+ − × ÷ ^ ( ) mod ! %` — `sin cos tan` (و`SHIFT` بيدّي الدالة العكسية،
`hyp` بيحوّلهم لـ hyperbolic) — `log` (أساس 10) و`ln` (أساس e)،
`SHIFT+log = 10^x`, `SHIFT+ln = eˣ` — `sqrt` (و`SHIFT+sqrt` = جذر تكعيبي)
— `x² x⁻¹ xʸ Logₓy(x,y)` — `nCr(n,r) nPr(n,r) gcd(a,b) lcm(a,b)` —
`Ans M+ M− RCL STO MC` — `Copy / Paste / History` — تبديل `DEG/RAD`.

## إيه اللي *مش* شغّال (زرار هيوريك رسالة "مش متاح" بدل ما يتفاعل بشكل وهمي)

`SOLVE`, التفاضل والتكامل (`d/dx`, `∫dx`), المصفوفات (`MATRIX`),
المتجهات (`VECTOR`), الأعداد المركبة (`CMPLX`), الإحصاء والتوزيعات
(`STAT`, `DISTR`), تحويل الدرجات/الدقائق/الثواني (`°′″`), `S⇌D`, `ENG`.
دي ميزات آلة حاسبة علمية حقيقية (CAS) بتاخد وقت أكبر بكتير من مجرد إضافة
زرار، فتم عمل stub ليها بدل ما تتعمل بشكل غير دقيق.

ملحوظة كمان: مفاتيح `SHIFT` في الصورة الأصلية كانت صغيرة وغير واضحة
بالكامل في الصورة، فاتعمل استخدامها بالـ convention القياسي لأي آلة
حاسبة علمية (SHIFT+Sin=Sin⁻¹, SHIFT+Log=10^x...) بدل محاولة قراءة كل
حرف صغير من صورة مش واضحة 100%.

## ملاحظة مهمة جدًا

الكود اتكتب في بيئة من غير Android SDK ومن غير إنترنت، فمقدرش أعمل
`build`/تشغيل فعلي واتأكد إنه هيتصرف بالظبط زي ما هو متوقع قبل الرفع.
لو طلع أي خطأ compile بسيط، افتح المشروع في Android Studio وهو هيوريك
السطر بالظبط، أو ابعتلي رسالة الخطأ وأصلحها.

## طريقة البناء (Build)

**الخيار 1 — GitHub Actions (تلقائي):**
كل `push` على `main` بيشغّل `.github/workflows/build.yml` اللي بيعمل
`assemble Debug` ويطلع ملف `app-debug.apk` جاهز تحت تبويب *Actions →
Artifacts* في المستودع.

**الخيار 2 — Android Studio:**
افتح المجلد كمشروع Android عادي، سيبه يعمل Sync (هيظبط `gradle wrapper`
لوحده)، وبعدين `Run` على أي جهاز/emulator بـ Android 5.0 (API 21) فأعلى.

## البنية

```
app/src/main/java/com/dualskin/calculator/
  CalculatorEngine.kt   ← تحليل وتنفيذ التعبير الرياضي
  MainActivity.kt       ← ربط الأزرار + منطق التبديل بين الواجهتين
app/src/main/res/layout/activity_main.xml  ← الواجهتين مع بعض
app/src/main/res/values/{colors,strings,themes}.xml
.github/workflows/build.yml  ← بناء APK تلقائي
```

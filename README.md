# Ночь и Орден

Android-игра в духе Vampire Survivors. Две фракции: **вампиры** и **Святой орден**.

Релизы: [github.com/t1ltof/NightAndOrder/releases](https://github.com/t1ltof/NightAndOrder/releases)

При запуске приложение само проверяет GitHub и предлагает скачать новую версию.

## Игра

- Шесть персонажей: Морван, Лилит, Никс / Луция, Хейл, Сера.
- Автоатака, опыт, выбор усилений, 8 минут до рассвета.
- Левый виртуальный стик. На эмуляторе — WASD.

Про яркость в игре нигде прямо не сказано. Это задумано.

## Сборка

Нужны JDK 17 и Android SDK. Путь к SDK — в `local.properties`.

```bat
gradlew.bat assembleRelease
```

APK: `app\build\outputs\apk\release\app-release.apk`

Для подписи релиза скопируйте `signing.properties.example` в `signing.properties` и положите keystore в `keystore/`.

# WeatherWidget

☂️ **傘が必要かひと目でわかる、シンプルで高機能な天気ウィジェットアプリ**

このアプリは、現在地の天気情報を取得し、ホーム画面に「気温」と「傘が必要かどうか」をアイコンで表示する Android ウィジェットアプリです。

## 主な機能
- **リアルタイム天気表示**: 現在の気温を表示。
- **傘ナビゲート**: 降水量データに基づき、傘を持っていくべきかをアイコン（☂️/⚠️）で通知。
- **ウィジェットカスタマイズ**: テキストの色（白/黒）を背景に合わせて選択可能。
- **低消費電力**: Open-Meteo API を使用し、効率的なデータ更新（30分間隔）を実現。

## 技術スタック
- **Language**: Kotlin
- **Networking**: Ktor Client (OkHttp engine)
- **Serialization**: kotlinx.serialization (JSON)
- **API**: [Open-Meteo API](https://open-meteo.com/) (Free, No API Key required)
- **Architecture**: Service-based background updates

## セットアップとビルド
1. Android Studio でプロジェクトを開く。
2. `./gradlew assembleDebug` でビルド。
3. 実機またはエミュレータにインストール後、ホーム画面を長押しして「WeatherWidget」を追加してください。

## プライバシーポリシー
このアプリは天気を取得するために位置情報を使用しますが、サーバーに保存することはありません。詳細は以下のページをご確認ください。
[プライバシーポリシーはこちら](https://zzzzico12.github.io/weatherwidget/PRIVACY_POLICY)

## ライセンス
Copyright © 2026 zzzzico12. All rights reserved.
Weather data by [Open-Meteo.com](https://open-meteo.com/).

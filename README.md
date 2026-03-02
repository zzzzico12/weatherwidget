# WeatherWidget

☂️ **傘が必要かひと目でわかる、シンプルで高機能な天気ウィジェットアプリ**

このアプリは、現在地の天気情報を取得し、ホーム画面に「気温」と「傘が必要かどうか」をアイコンで表示する Android ウィジェットアプリです。

## 主な機能
- **リアルタイム天気表示**: 現在の気温を表示。
- **傘ナビゲート**: 降水量データに基づき、傘を持っていくべきかをアイコン（☂️/⚠️）で通知。
- **ウィジェットカスタマイズ**: テキストの色（白/黒）を背景に合わせて選択可能。
- **効率的な更新**: Android 14+ に最適化されたフォアグラウンドサービスによる確実な更新。
- **プライバシー配慮**: 位置情報は天気取得のみに使用し、外部サーバーへの保存は行いません。

## 最新のアップデート (v1.1)
- **Android 14/15 対応**: 最新の Android OS でのバックグラウンド実行制限（Foreground Service 制限）をクリアし、安定性を向上。
- **権限管理の改善**: 位置情報権限（今回のみ/許可しない）に対する誘導ダイアログを追加。
- **コード最適化**: Activity Result API を導入し、よりモダンで堅牢な設計に刷新。

## 技術スタック
- **Language**: Kotlin
- **Networking**: Ktor Client (OkHttp engine)
- **Serialization**: kotlinx.serialization (JSON)
- **API**: [Open-Meteo API](https://open-meteo.com/) (Free, No API Key required)
- **Architecture**: Service-based background updates with Foreground Service

## セットアップとビルド
1. Android Studio でプロジェクトを開く。
2. `./gradlew assembleDebug` でビルド。
3. 実機またはエミュレータにインストール後、ホーム画面を長押しして「WeatherWidget」を追加してください。

## ライセンス
Copyright © 2026 zzzzico12. All rights reserved.
Weather data by [Open-Meteo.com](https://open-meteo.com/).

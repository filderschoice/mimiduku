---
name: mimiduku-change
description: みみづく（本アプリ）へ機能追加・不具合修正・検知ルールや文言の変更を入れるときの手順と点検表。判定ロジックの置き場所と単体テスト、版の差（FeatureSet）、rules.json と DetectionRules.DEFAULT の同時更新、strings.xml、開示文の版数、実機確認の起票、UI 設計書の追従、記録ファイルの更新先を、正本の参照先つきでまとめる。「BL-xxx を実装して」「検知ルールを足して」「文言を直して」「不具合を直して」等、mobile/ 配下のコードを変える依頼で参照する。
---

# みみづくの変更手順と点検表

本アプリの変更で守る規範は DESIGN.md・README.md・strings.xml のコメント・docs/TESTING.md に分かれており、毎回探し直すと
見落としが出ます。本スキルは**どこを見れば何が決まっているか**の索引です。規範の本文は正本に置いたままにし、ここへ複製しません
（2か所に書くと片方だけ更新されて食い違うため）。正本と本スキルが食い違う場合は正本に従い、本スキルを直してください。

ブランチ・コミット・品質ゲートは `CLAUDE.md` と `.github/copilot-instructions.md`、自律ループ実行モードでの進め方は
Skill `autonomous-loop` が正本です。

## 1. 着手前に読む

| 読むもの | 何のため |
| --- | --- |
| 対象の BACKLOG 項目（`docs/records/managed/BACKLOG.md`） | タスク内容・完了条件・根拠（既定値の選択理由）が仕様の正本 |
| `docs/records/managed/DESIGN.md` の該当 FR の行と「設計方針」 | 既存仕様の維持（明示されていない要件は変えない） |
| `docs/records/managed/DESIGN.md`「エージェント実装指示」 | 追加実装時の出力要件 |

## 2. 変更の種類ごとの点検表

| 変更するもの | 守ること | 正本 |
| --- | --- | --- |
| 判定・分岐 | Android 非依存のクラスへ置き、JVM の単体テストを足す。保存は `StringStore` 経由にし、テストでは `MemoryStringStore`（`mobile/src/test/.../storage/`）を使う | DESIGN.md「設計方針」「エージェント実装指示」、docs/TESTING.md 1章のテスト一覧 |
| 版（GitHub 版・Play 版）で違う機能 | `BuildConfig` を直接見ず `FeatureSet` に項目を足し、`FeatureSetTest` を更新する | DESIGN.md の FR-19 |
| 検知するパッケージ名・スケアウェア文言 | `mobile/src/main/assets/rules.json` と `DetectionRules.DEFAULT` を同時に更新し、`RulesLoaderTest` で一致を確かめる | README.md「検知ルールの更新」、DESIGN.md「エージェント実装指示」 |
| スケアウェア文言の追加 | 本アプリ自身の文言（アクセシビリティ設定の説明文）が照合語に当たらないことを `OwnTextScarewareTest` で確かめる | `OwnTextScarewareTest` の KDoc（BL-060 の経緯） |
| 対応ブラウザのアドレスバー | `UrlBarIds` にビュー ID を足し、`UrlBarIdsTest` を更新する。分からないブラウザは確認を省かない側に倒す | `UrlBarIds` の KDoc、DESIGN.md の FR-25 |
| 画面の文言 | `mobile/src/main/res/values/strings.xml` だけを変える（コードへ直書きしない） | strings.xml 冒頭のコメント、DESIGN.md「エージェント実装指示」 |
| 開示文（`disclosure_body` など開示と同意の文言） | `ConsentStore.CURRENT_VERSION` を上げるか判断し、`docs/PRIVACY.md` と `docs/PLAY_LISTING.md`「Play Console の申告で使う要点」を合わせる。版数を上げると既存の利用者は再同意まで保護が止まるため、判断が要る場合は `状態: 要確認` で起票する | DESIGN.md の FR-20、strings.xml の開示文のコメント、BL-074 |
| 読み取る・保存する情報 | `INTERNET` を宣言しない。画面の文字や URL を保存・送信しない。変えるなら `docs/PRIVACY.md` を更新し、プライバシーに関わる判断は要確認にする | DESIGN.md「非機能要件」、docs/PRIVACY.md 1〜3章、`rules/guardrails-app.v1.md` |
| UI（画面・バナー・バッジ・アイコン） | `docs/design/UI_DESIGN.html` の追従が要るかを判断し、要るなら更新する（手順はユーザー共通スキル `ui-design-doc-html`） | docs/design/UI_DESIGN.html |

## 3. 実機確認が要る変更

単体テストで確かめられない挙動（AccessibilityService のイベント、オーバーレイの表示、他アプリへの遷移）は、
`docs/TESTING.md` 2章へ確認項目（No・手順・期待値）を足し、BACKLOG へ `区分: 人手検証` で起票します（既存の人手検証項目へ
足すなら、その項目の完了条件は変えずに TESTING.md の節を参照させる）。自律ループ実行モードでは実機確認を完了判定から除き、
確認の実施は実装とセッションを分けます（Skill `autonomous-loop`「利用量を抑える」）。実施の手順は Skill `mimiduku-device-verification`。

## 4. 品質ゲートと記録

1. `CLAUDE.md`「本リポジトリの品質ゲート定義」の全コマンドを実行する（Kotlin・Gradle に触れた変更では Gradle 系を省略しない）。
   出力を会話へ載せたくない場合はサブエージェント `quality-gate-runner`、差分の点検は `diff-reviewer` を使う。
2. 記録ファイルを `docs/records/spec/FORMAT.md` に従って更新する。コード修正を伴う完了では、`DESIGN.md` の該当 FR の行
   （新しい要件なら FR を追加し、「エージェント実装指示」の FR の範囲も直す）と `EXECUTE.md` の両方を更新する。
   実機確認が残る場合は BACKLOG から削除せず `進行中` で残す（ユーザー共通スキル `guardrail-records-update` 2.3）。
3. `git add` の後に `python scripts/scan-secrets.py` を実行してからコミットする。

---
name: mimiduku-device-verification
description: みみづく（本アプリ）を実機で確認するときの、このアプリ固有の手順。BACKLOG の人手検証の項目と docs/TESTING.md 2章の節の対応、確認する版（GitHub 版・Play 版・旧版の APK）の選び方、スケアウェア文言のテストページの使い方、検知ログをテキストで読む方法、確認後に端末の設定を元へ戻すこと、結果の記録先（TESTING.md 4章と BACKLOG）をまとめる。「実機で確認して」「BL-009 を確かめて」「人手検証を進めて」「TESTING.md の T 系を実機で」等、本アプリの実機確認を始める前に参照する。adb 接続そのものはユーザー共通スキル adb-device-connection、利用量の抑え方は verification-cost-control に委ねる。
---

# みみづくの実機確認

本アプリの実機確認で、**どの項目を・どの版で・どう確かめ・どこへ書くか**をまとめます。確認項目（手順と期待する結果）の正本は
`docs/TESTING.md` 2章で、本スキルへは複製しません。次の2つはユーザー共通スキルが正本で、ここでは扱いません。

- adb 接続・インストール・複数台の指定・サンドボックスでの adb サーバーの消滅: `adb-device-connection`
- スクリーンショットの読み方、logcat・dumpsys の絞り方、120秒の制限: `verification-cost-control`

**未検証の注意**: 本スキルは BL-073 で作成し、全体を通して実機で使ったことはまだありません。2026-11-09 の確認（TESTING.md 4章）で
`adb reverse` による配信と検知ログの読み出しは行っていますが、`tools/verification/scareware-page/` のページと 2.2 手順3のコマンド
（`--bind 127.0.0.1 --directory`）はその後に用意したもので、端末では未確認です（PC 上で3つのページが HTTP 200 で返ることだけ確かめた）。
初回の利用で食い違いがあれば本スキルと TESTING.md を直してください。

## 1. 始める前に

- 実装の会話とはセッションを分ける（Skill `autonomous-loop`「利用量を抑える」）。自律ループ実行モードの中では実機を操作しない。
- **実際の広告はクリックしない**（TESTING.md 2.1）。
- 端末の現在の設定を控える: 利用モード（こども・おとな）、PIN の有無、バッジの表示、通知のスイッチ、確認を省く対象（アプリ・サイト）。
  確認の後でこの状態へ戻す（6章）。PIN を新しく設定した場合は、戻すときに削除する（削除すると登録済みのサイトも消える。X13）。

## 2. BACKLOG の人手検証と TESTING.md の対応

完了条件は BACKLOG の各項目が正本です。下表は着手時の目安で、BACKLOG と食い違えば BACKLOG に従い、本表を直します。

| BACKLOG | TESTING.md | 主な版 |
| --- | --- | --- |
| BL-009 | 2.3（T1〜T16）、2.8（ログの確認） | GitHub 版。T16 は版数 1 の旧 APK も使う |
| BL-039・BL-045 | 2.6（U1〜U7）。BL-045 は U1〜U5 の確認時に ⓘ の操作と読み上げも見る | GitHub 版 |
| BL-043・BL-054 | 2.7（B1〜B8）。BL-054 は B 系の確認時に「×」と大きいバッジも見る | GitHub 版 |
| BL-056 | 2.9（X1〜X15） | GitHub 版（X4 はこどもモード） |
| （完了済みの BL-023） | 2.4（P 系）・2.5（G 系） | P 系は GitHub 版、G 系は Play 版。不具合の修正後の再確認で使う |

BL-025・BL-031・BL-038・BL-052・BL-057 は TESTING.md の節を持たない人手検証（調査・申請・撮影・公開の確認）で、本スキルの対象外です。

## 3. 入れる版

- 既定は GitHub 版の debug（`com.mimiduku.mobile.full.debug`）。ビルドとインストールのコマンドは README.md「インストール（サイドロード）」。
- G 系（Play 版の確認）は Play 版の debug（`com.mimiduku.mobile.debug`）。両方を入れた場合、**アクセシビリティは片方だけオン**にする
  （README.md の指示。両方オンにしたときの挙動は未確認）。
- T16 は、版数 1 の APK（BL-074 より前のコミット）を同じ PC でビルドして入れ、同意してから現在の APK を `adb install -r` で上書きする。
  旧版のビルドは `git worktree` で別のフォルダに作ると、作業中のツリーを汚さない。
- 確認の前に、最後にビルドした APK が確認したいコミットのものかを `git log -1` とビルドの時刻で確かめる。

## 4. 素材

- スケアウェア文言（T9〜T11）: `tools/verification/scareware-page/` を TESTING.md 2.2 手順3のとおり配信する。読み込み直後の版
  （`index.html`）、スクロールで出る版（`scroll.html`）、T11 のリンク先（`safe.html`）がある。ページの文言は架空で、照合語は
  `rules.json` から取っている。文言を変えたら単体テスト `ScarewarePageTest` を通してから端末で使う。
- 外部遷移（T1 系）: TESTING.md の `adb shell am start` のコマンドを使う。Chrome が起動済みだと検知されないことがある
  （BL-056 に既知）ため、T1 系は `adb shell am force-stop com.android.chrome` などで Chrome を止めてから行う
  （2026-11-08 の記録では `am kill` を使用）。

## 5. 確かめ方（画像を減らす）

- 検知の有無と介入の内容は、画面写真より検知ログ（テキスト）で確かめる。版ごとに applicationId が違う。

  ```sh
  adb shell run-as com.mimiduku.mobile.full.debug cat files/detection-log.json   # GitHub 版
  adb shell run-as com.mimiduku.mobile.debug cat files/detection-log.json        # Play 版
  ```

  `kind`（EXTERNAL_TRANSITION・SCAREWARE）と `action`（BACK_AND_WARN・WARN_ONLY・ASK_USER・SKIPPED_EXCLUDED）を期待値と照らす。
  画面の文字や URL が含まれていないこと（TESTING.md 2.8）もこの出力で確かめる。
- バナーの文言・ボタンの有無は、バナーが消える前に取る必要がある（2026-11-08 の T3 は消えた後で目視できず、ログで代替した）。
  画面の取得は `verification-cost-control` の方法で最小限にする。
- 人の目でしか判断できない項目（タップ感、読みやすさ、TalkBack の読み上げ）は、ユーザーに見てもらい結果を聞く。

## 6. 元へ戻す

確認の後、1章で控えた状態へ戻す（利用モード・PIN・バッジの表示・通知・確認を省く対象）。テストページのサーバーを止め、
`adb reverse --remove tcp:8000` を実行する。端末の回転・ナビゲーションの種類・ダークテーマ・フォントの大きさなど、
確認のために変えた端末側の設定も戻す。戻したことを記録の備考に書く（4章の既存の記録と同じ書き方）。

## 7. 記録

1. `docs/TESTING.md` 4章の表へ1行足す（実施日・端末（版・接続・ブラウザの版）・Android の版・ナビ・No ごとの OK / NG / 未実施・備考）。
   未実施の項目も「未実施」と書く（後から何が残っているか分かるように）。
2. NG は TESTING.md 3章のとおり BACKLOG へ起票する（端末名・Android の版・ナビ・No・実際の結果）。修正後の再確認は同じ行の備考へ書く。
3. BACKLOG の人手検証の項目は、完了条件の全文が満たされたときだけ削除する。一部だけなら削除せず、実施した範囲を `タスク内容` の末尾か
   `根拠` へ追記する（書き方は FORMAT.md とユーザー共通スキル `guardrail-records-update` 2.3）。
4. 実機確認だけでコードを変えていない場合、EXECUTE.md は更新せず CHANGELOG.md へ書く。確認で分かった事実（例 ブラウザごとの
   読み取りの可否）が設計に関わる場合は DESIGN.md の該当 FR へ反映する。

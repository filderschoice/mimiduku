# 変更履歴

このリポジトリの主要な変更は本ファイルに記録します。

## Unreleased

- UI を整理した（ネイビー基調のダーク対応テーマ、ホーム・設定・このアプリについての画面構成、歯車と「⋮」メニュー、オーバーレイの主ボタンの配色、丸みのあるバッジ・アイコン）。BL-032〜BL-036
- UI デザイン設計書（docs/design/UI_DESIGN.html）を新しい UI に追従させ、所見の対応状況を反映した（BL-037）
- PLAN.md をもとに MVP（外部遷移の検知とスケアウェア文言の警告）のタスクを BACKLOG へ分解した
- PLAN.md 8章への回答と MVP の設計を docs/design/MVP_DESIGN.md にまとめた（大人の利用を考慮した利用モードを含む）
- README.md（セットアップ・サイドロード・更新・ロールバック）と docs/TESTING.md（単体テストと実機確認の手順）を追加した
- DESIGN.md を MVP の実装済み内容（FR-1〜FR-9）で最新化した
- 第2段階（保護者 PIN を先に、広告画面の判定・タップシールド・代理タップ・バッジ）を BL-014〜BL-023 へ分解した
- 脆弱性チェックは未導入のまま、リポジトリ公開後に GitHub の Dependabot で確認する方針とした（BL-011 を閉じた）
- README・docs/TESTING.md・DESIGN.md を第2段階（PIN・一時停止・ログ閲覧・広告画面・シールド・代理タップ・バッジ）に合わせて更新した
- Google Play のポリシー調査と配布の構成案の作成を BL-026 として登録した
- Google Play のポリシーを調べ、Play 版（機能限定）と GitHub 版（全機能）に分ける案を含む配布の構成案を docs/design/DISTRIBUTION.md にまとめた（BL-026）
- 全画面広告では正常に見えても悪質な場合があるため、GitHub 版では全画面広告への介入を残す方針とした（ユーザー決定）
- Play 版と GitHub 版を同じソースからフラグで出し分ける作業を BL-028〜BL-031 へ分解した
- プライバシーポリシー（docs/PRIVACY.md）と Google Play のリスティング文案（docs/PLAY_LISTING.md）を追加した（BL-030）

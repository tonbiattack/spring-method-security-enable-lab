# 題材企画: `@PreAuthorize` を付けたのに経理以外が請求書を承認できる

## 対象

| 項目 | 内容 |
| --- | --- |
| 対象言語 | Java 21 |
| 対象読者 | Spring Securityのアノテーション認可を導入・レビューする中級開発者 |
| 難易度プロファイル | 実践・上級 |
| 選定理由 | 宣言したアノテーションの存在ではなく、実行時に認可アドバイザが有効かを例外と最終状態で検証する。 |
| 実行基盤 | Maven 3.8.7、Spring Boot 3.5.0、Spring Security、JUnit 5、spring-security-test |
| フレームワーク非依存性 | 該当しない。Spring Securityのメソッド認可設定を直接扱う。 |

## 学習する契約

> `VIEWER` ロールの利用者が `invoice-001` を承認しようとしたとき、期待する結果は `AccessDeniedException` と未承認状態である。しかしバグ状態では例外なしで処理が完了し、請求書は承認済みになる。

### 対象の直接原因

`@PreAuthorize` が付いたサービスメソッドを用意したが、認可アドバイザを有効にする `@EnableMethodSecurity` が設定クラスにない。[1]

### 対象外

HTTPフィルタ、URL認可、JWT、OAuth2、ユーザー登録、権限付与、SpELの複雑な式、所有者チェック、行レベル認可、自己呼び出しによるAOPプロキシ迂回は扱わない。

## 再現設計

| 要素 | 決定 |
| --- | --- |
| 公開境界 | `InvoiceApprovalService#approve` |
| 入力・初期状態 | 承認済み集合を空にし、`VIEWER` が `invoice-001` の承認を試みる。 |
| Redの観測 | 例外なしと、`isApproved("invoice-001") == true` を同時に観測する。 |
| 最終観測 | `ApprovalRepository#isApproved` で承認済み状態を独立して確認する。 |
| 対照ケース | `FINANCE` ロールが `invoice-002` を承認できること。 |
| 固定状態の検証コマンド | `mvn --batch-mode test` |
| バグ状態の確認コマンド | `mvn --batch-mode -Dtest=InvoiceApprovalServiceIntegrationTest test` |

## 仮説

| 仮説 | どう検証または除外するか |
| --- | --- |
| SpEL式が誤っている | `FINANCE` の対照ケースを追加する。 |
| テスト認証コンテキストが作られていない | `VIEWER` と `FINANCE` の差を `@WithMockUser` で観測する。 |
| メソッド認可が有効化されていない | 設定境界と無権限呼出しの最終状態を確認する。 |

## 予定した履歴

| 順序 | コミットの目的 | 期待する状態 |
| --- | --- | --- |
| 1 | `371ed30`：メソッド認可が有効化されていない状態を再現する | 無権限のテストで例外なし・承認済み状態となり失敗する。 |
| 2 | `c0736fa`：メソッド認可を有効化する | 同じテストと全テストが成功する。 |

## References

[1] [Spring Security Reference — Method Security](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html)

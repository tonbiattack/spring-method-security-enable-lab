# E003: `@PreAuthorize` を付けたのに経理以外が請求書を承認できる

このリポジトリは、Java／Spring Bootで `@PreAuthorize` を付けたサービスメソッドが、メソッド認可の有効化漏れによって無保護になる不具合を学ぶ実行可能なデバッグラボです。失敗する統合テスト、最終状態の確認、最小修正、回帰テストを含みます。既定ブランチは修正済みで、バグ状態はGit履歴に残しています。

> 学習する契約：`FINANCE` ロールを持たない利用者は請求書を承認できず、承認済み状態を残してはならない。

## 学習の進め方

| 段階 | 実施内容 | 観測すること |
| --- | --- | --- |
| 再現 | バグコミットで統合テストを実行する | 例外が送出されず、請求書が承認済みになる |
| 観測 | 例外とリポジトリの最終状態を別々に確認する | `@PreAuthorize` の宣言だけでは認可が始まらない |
| 修正 | `@EnableMethodSecurity` を追加する | Spring Securityがメソッド認可のアドバイザを有効にする |
| 回帰防止 | 同じ統合テストを再実行する | 権限なしは拒否され、経理権限は承認できる |

## 収録済み教材

| ID | テーマ | バグ状態の観測 | 修正後に守る契約 |
| --- | --- | --- | --- |
| E003 | Springメソッド認可の有効化 | 例外なし、承認済み状態が残る | 権限なしは拒否され、状態を変えない |

## 必要な環境

| 項目 | 本ラボで検証したバージョン |
| --- | --- |
| JDK | 21.0.11 |
| Maven | 3.8.7 |
| Spring Boot | 3.5.0 |
| Spring Security | Spring Bootの依存関係管理に従う |

## 修正後のテストを実行する

```bash
mvn --batch-mode test
```

テストは、`VIEWER` が `AccessDeniedException` で拒否されることと、承認済み状態が残らないことを同時に確認します。対照として、`FINANCE` ロールが承認を成功させることも確認します。

## バグを自分で再現する

```bash
git switch --detach 371ed30
mvn --batch-mode -Dtest=InvoiceApprovalServiceIntegrationTest test
# 例外が送出されず、承認済み状態も true になって失敗する

git switch main
mvn --batch-mode test
# BUILD SUCCESS
```

## プロジェクト構成

```text
src/main/java/com/example/methodsecurity/
├── MethodSecurityLabApplication.java # @EnableMethodSecurityを配置する設定境界
├── InvoiceApprovalService.java       # @PreAuthorizeを付けた承認操作
└── ApprovalRepository.java           # 承認済み状態の独立した観測
src/test/java/com/example/methodsecurity/
└── InvoiceApprovalServiceIntegrationTest.java

docs/
├── topic-brief.md
├── novelty-report.md
├── debugging-record.md
├── bug-state-test-output.log
└── fixed-state-test-output.log
```

Spring Securityのメソッド認可は、`@EnableMethodSecurity` を設定クラスへ付けて有効化できます。[1] この有効化がなければ、Spring Boot Starter Securityを導入してもメソッドレベルの認可は既定で有効になりません。[2]

詳細な仮説比較と証拠は、[デバッグ記録](docs/debugging-record.md)を参照してください。

## References

[1] [Spring Security Reference — Method Security](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html)

[2] [Spring Security API — EnableMethodSecurity](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/config/annotation/method/configuration/EnableMethodSecurity.html)

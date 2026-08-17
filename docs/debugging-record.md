# E003: `@PreAuthorize` を付けたのに経理以外が請求書を承認できる

## 目的

`FINANCE` ロールを持たない利用者が請求書承認を呼び出した場合、`AccessDeniedException` で拒否され、承認済み状態が残らないことを契約とする。本ラボでは、`@PreAuthorize("hasRole('FINANCE')")` を宣言したサービスを、実際のSpring Securityコンテキストと `@WithMockUser` で検証する。

## 最初に観測した事実

バグ状態はコミット `371ed30`（`メソッド認可が有効化されていない状態を再現する`）に保存した。`InvoiceApprovalService#approve` には `@PreAuthorize` が付いているが、アプリケーションに `@EnableMethodSecurity` は付いていない。

```bash
git switch --detach 371ed30
mvn --batch-mode -Dtest=InvoiceApprovalServiceIntegrationTest test
```

テストでは、`VIEWER` ロールの利用者が `invoice-001` の承認を試みる。例外と最終状態をSoft Assertionsで同時に観測した。

| 観測項目 | 期待 | 実際 | 根拠 |
| --- | --- | --- | --- |
| 直接結果 | `AccessDeniedException` | 例外なし | 失敗テスト |
| 最終状態 | `isApproved("invoice-001") == false` | `true` | 失敗テスト |
| 対照ケース | `FINANCE` は承認できる | 成功 | 同じ統合テスト |

```text
Multiple Failures (2 failures)
[経理権限がなければAccessDeniedExceptionで拒否されること]
Expecting actual not to be null

[拒否された操作は承認済み状態を残さないこと]
Expecting value to be false but was true
```

これにより、認可例外を捕捉できなかっただけでなく、保護されるべき操作が実際に状態を変更したことを確認できる。

## テストの境界

`@SpringBootTest` と `spring-security-test` の `@WithMockUser` を用い、Springが管理するサービスBeanを実際に呼び出す。ユニットテストでアノテーションを反射して存在確認するだけでは、認可アドバイザが有効か、呼び出しが拒否されるか、状態変更が止まるかを検証できないためである。

| 要素 | 決定 |
| --- | --- |
| 公開境界 | `InvoiceApprovalService#approve` |
| 初期状態 | `ApprovalRepository` の承認済み集合を空にする |
| 不正な入力 | `VIEWER` ロールで `invoice-001` を承認する |
| 直接観測 | 送出された例外 |
| 最終観測 | `ApprovalRepository#isApproved` による承認済み状態 |
| 対照ケース | `FINANCE` ロールで `invoice-002` を承認する |

## 仮説と切り分け

| 仮説 | 予測 | 最小実験 | 結果 | 判定 |
| --- | --- | --- | --- | --- |
| `@PreAuthorize` のSpEL式が誤っている | `FINANCE` も拒否されるか、式解決例外になる | `FINANCE` の対照テストを実行する | 承認できる | 棄却 |
| テストの認証コンテキストが作られていない | `VIEWER` と `FINANCE` の区別がない | `@WithMockUser` の異なるロールを比較する | `VIEWER` も処理本体へ到達する | 棄却 |
| メソッド認可が有効化されていない | `@PreAuthorize` があっても両ロールが実処理へ到達する | 設定クラスのアノテーションと最終状態を確認する | `@EnableMethodSecurity` がなく、`VIEWER` の状態変更を観測 | 採用 |

## 原因

`@PreAuthorize` は、メソッド認可が有効なときにSpring AOPの認可アドバイザが解釈する宣言である。Spring Securityの公式リファレンスは、メソッド認可を `@EnableMethodSecurity` で有効化できること、またSpring Boot Starter Securityはメソッドレベル認可を既定で有効にしないことを明記している。[1]

バグ状態では、`InvoiceApprovalService` はSpring Beanであり `@PreAuthorize` も存在するが、認可アドバイザを登録する `@EnableMethodSecurity` がない。そのため、`VIEWER` の呼び出しがサービス本体まで到達し、承認状態を残した。

## 修正

`MethodSecurityLabApplication` に `@EnableMethodSecurity` を追加した。

```java
@SpringBootApplication
@EnableMethodSecurity
public class MethodSecurityLabApplication {
}
```

この変更により、`@PreAuthorize`、`@PostAuthorize`、`@PreFilter`、`@PostFilter` のメソッド認可が有効になる。[2] 本ラボでは `@PreAuthorize` を用いているため、`prePostEnabled` は既定値のままとした。`EnableMethodSecurity` のAPIで `prePostEnabled` の既定が `true` であることを確認できる。[2]

修正後は、`VIEWER` が呼び出すとサービス本体の前で `AccessDeniedException` が送出され、リポジトリは空のままである。`FINANCE` の対照ケースはこれまで通り承認済み状態を残す。

## 再現手順

```bash
# バグ状態：無権限の呼出しが例外なしで承認済み状態を残す
git switch --detach 371ed30
mvn --batch-mode -Dtest=InvoiceApprovalServiceIntegrationTest test

# 修正状態：無権限は拒否され、経理権限だけが承認できる
git switch main
mvn --batch-mode test
```

## 再発防止テスト

修正前から同じ `InvoiceApprovalServiceIntegrationTest` を保持している。権限なしケースは、例外と状態を別々に検証するため、将来例外だけを返して状態が変わる、または状態だけが偶然不変の不完全な修正を検出できる。

| テスト | 固定する契約 |
| --- | --- |
| `経理権限を持たない利用者は請求書を承認できない` | `VIEWER` は拒否され、`invoice-001` は未承認のままである。 |
| `経理権限を持つ利用者は請求書を承認できる` | `FINANCE` は処理を実行し、`invoice-002` を承認済みにする。 |

```bash
# バグ状態
git switch --detach 371ed30
mvn --batch-mode -Dtest=InvoiceApprovalServiceIntegrationTest test

# 修正状態
git switch main
mvn --batch-mode test
```

修正コミットは `c0736fa`（`メソッド認可を有効化する`）である。実行出力は `docs/bug-state-test-output.log` と `docs/fixed-state-test-output.log` に保存している。

## 適用範囲と注意点

本ラボは、Springコンテキスト内のサービスBeanに `@PreAuthorize` を付け、`@EnableMethodSecurity` を有効化する最小構成を扱う。HTTPフィルタのURL認可、認証プロバイダ、JWT、権限の発行、複雑なSpEL式、データ所有者による行レベルの認可は対象外である。

また、認可アノテーションを付けていないメソッドは、メソッド認可だけでは保護されない。Spring Securityの公式リファレンスも、アノテーションベースのメソッド認可では未注釈メソッドが保護されない点に注意を促している。[1] 実アプリケーションでは、サービス層の認可とHTTP層の包括的な認可規則を要件に応じて設計する必要がある。

## References

[1] [Spring Security Reference — Method Security](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html)

[2] [Spring Security API — EnableMethodSecurity](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/config/annotation/method/configuration/EnableMethodSecurity.html)

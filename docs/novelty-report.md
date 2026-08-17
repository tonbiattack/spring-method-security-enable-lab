# 題材重複調査レポート: `@PreAuthorize` を付けたのに経理以外が請求書を承認できる

## 調査対象

| 項目 | 内容 |
| --- | --- |
| 対象言語 | Java 21 |
| 難易度プロファイル | 実践・上級 |
| 候補題材 | Spring Securityのメソッド認可が有効化されていない問題 |
| 観測可能な契約 | `VIEWER` が `invoice-001` を承認しようとしたとき、拒否され未承認のままであるべきだが、バグ状態では例外なしで承認済みになる。 |
| 直接原因 | `@EnableMethodSecurity` がなく、`@PreAuthorize` を処理する認可アドバイザが有効でない。 |
| カタログ更新日時 | 該当なし。`/home/ubuntu/repository-catalog/data/repositories.json` は環境内に存在しなかった。 |
| 検索語 | `@PreAuthorize`, `@EnableMethodSecurity`, `Spring Security`, `AccessDeniedException`, `authorization`, `role` |

## 調査方法と制約

指定スキルの標準カタログは利用できなかった。そのため、カタログの自動更新と語彙スコアリングは実施していない。代替として、ユーザー指定の `qiita` リポジトリ全体を `@PreAuthorize`、`@EnableMethodSecurity`、`Spring Security`、`MethodSecurity`、`AccessDeniedException`、`@RequestParam`、`Bean Validation` で横断検索した。該当するSpring Securityメソッド認可の記事は見つからなかった。

本レポートの結論は、利用可能だったQiita記事群と、この作業中に作成したラボに対する手動比較に限る。ユーザーの全リポジトリを網羅するカタログに基づく新規性主張ではない。

## 近接候補の比較

| 既存教材・記事 | 既存の原因 | 既存の実境界・最終観測 | 今回の差分 | 判定 |
| --- | --- | --- | --- | --- |
| `spring-transaction-self-invocation-lab` | 同一Beanの自己呼び出しが `@Transactional` プロキシを迂回する | 例外後の残高再読込でロールバックを確認する | 今回は認可アドバイザの有効化を扱い、無権限呼出しの例外と承認状態を観測する。 | 重複なし |
| `spring-async-transaction-boundary-lab` | 非同期ワーカーがコミット前にデータを読む | ワーカーの可視性とH2の最終状態を確認する | 今回は非同期やトランザクションを使わず、メソッド呼出し前のロール認可を扱う。 | 重複なし |
| Springの`@Async`自己呼び出し記事 | 同一Bean内の呼出しが `@Async` プロキシを迂回する | 実行スレッドを観測する | 今回はSpring AOPを使うが、原因は自己呼び出しではなく認可機能の未有効化であり、例外と状態を観測する。 | 重複なし |
| NestJSの`@Roles`が無視される記事 | ガードまたはReflector設定によりNestJSのメタデータが認可へ接続されない | HTTPレスポンスとガードの実行を観測する | 今回はSpring Securityの `@PreAuthorize`、Spring Bean、`@EnableMethodSecurity`、サービス層の状態変更を扱う。 | 近接だが重複なし |

## 結論

作成する。

今回の題材は、直接原因がSpring Securityのメソッド認可有効化、実境界がサービスBeanの `@PreAuthorize` 呼出し、観測契約が無権限時の例外と最終承認状態、最小修正が `@EnableMethodSecurity` の追加である。既存のトランザクション・非同期・Spring AOPの題材と、教育上意味のある差分がある。

## 作成前チェック

- [ ] カタログを手動更新して検証した。環境内にカタログがなく未実施。
- [ ] 語彙的な近接候補を抽出した。カタログ不在のため未実施。
- [x] 利用可能なQiita記事の近接候補本文と検索結果を確認した。
- [x] 同じ失敗を名称だけ変えて再実装していない。
- [x] 再現、観測、最小修正、回帰テストの実装計画を立てた。

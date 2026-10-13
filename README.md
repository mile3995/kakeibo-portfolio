# くらしの家計簿

Java と Spring Boot で作った、収入と支出を記録する家計簿のアプリです。アカウントごとにデータを分けて保存し、月ごとの集計やカテゴリー別グラフを確認できます。

## 主な機能

- アカウント登録とログイン
- 収入・支出の登録と一覧表示
- 支出の削除
- 月ごとの収支と貯金残高の表示
- 支出のカテゴリー別集計と円グラフ
- ライトモードとダークモードの切り替え
- 以前の CSV データの取り込み（デモでは無効です）

## 使用技術

- Java 25
- Spring Boot
- Spring Security
- Spring Data JPA
- H2 Database（ローカル実行）
- PostgreSQL（公開環境向け）
- HTML / CSS / JavaScript

## フォルダー構成

```text
src/main/java/com/example/kakeibo/
├── account/    アカウント登録、ログイン、利用者情報
├── config/     Spring Security とアプリ設定
├── expense/    支出の登録、一覧、削除
├── importing/  旧 CSV データの確認と取り込み
└── income/     収入の登録と一覧

src/main/resources/
├── application.properties
└── static/
    ├── css/    ページごとのデザイン
    ├── js/     ページごとの画面処理
    ├── index.html
    ├── login.html
    ├── signup.html
    └── import.html
```

## Windows でローカル起動

1. Java 25 をインストールします。
2. このフォルダーでコマンドプロンプトを開きます。
3. 次のコマンドを実行します。

```bat
mvnw.cmd spring-boot:run
```

4. ブラウザーで `http://localhost:8080` を開きます。
5. アカウントを作成してログインします。

終了するときは、起動中のコマンドプロンプトで `Ctrl + C` を押します。

## 公開デモについて

Render と Neon の無料枠を使ったサンプル公開を想定しています。Render の無料サービスはアクセスがないと休止する場合があります。公開デモには実際の家計情報、個人情報、他サービスで使っているパスワードを入力しないでください。公開方法や設定は [DEPLOY_DEMO.md](DEPLOY_DEMO.md) を参照してください。

## 注意

- 公開デモでは CSV 取り込み機能を無効にします。
- `backups/` と CSV・環境変数ファイルは公開リポジトリに含めない設定です。
- デプロイ先の接続情報やパスワードをソースコードに書かないでください。


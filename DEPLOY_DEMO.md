# 無料デモを公開する手順

公開コード: [mile3995/kakeibo-portfolio](https://github.com/mile3995/kakeibo-portfolio)

このリポジトリにはRenderの設定ファイル `render.yaml` があり、RenderのFree Web ServiceとNeonのFree PostgreSQLで公開する準備ができています。Render/Neonのアカウントとクラウド上のサービスは、まだ作成していません。

## 公開デモの使い方と注意

- サイトは誰でも開けます。利用者は自分でアカウント登録できます。収支データはアカウントごとに分けて保存します。
- 公開デモには架空のサンプルデータだけを入れてください。実際の収入・支出、氏名、個人情報、他サービスで使っているパスワードは登録しないでください。
- RenderのFree Web Serviceは15分アクセスがないと休止し、次にアクセスしたとき起動に約1分かかる場合があります。ローカルファイルは再起動や休止時に消えるため、この構成ではNeonのPostgreSQLに保存します。
- NeonのFreeプランは月額0ドルですが、プロジェクトあたりのコンピュート時間・保存容量・通信量に上限があります。上限やプランの内容は変わるので、作成時に公式ダッシュボードで確認してください。
- RenderのFree Service自体は無料ですが、無料枠を超えた帯域やビルド利用は、支払方法が登録されている場合に追加請求となることがあります。追加料金を避けたい場合は支払方法を登録せず、Freeプランを選び、有料プランへ変更しないでください。ダッシュボードがカード登録を求める場合はいったん止めてください。
- 公開デモではCSV取り込み機能を無効にします。

## 1. Neonでデータベースを作る

1. [Neon](https://neon.com/) にサインインします。アカウントがなければ自分で作成します。
2. 新しいPostgreSQLプロジェクトを作成し、プランは **Free** を選びます。プロジェクト名は `kakeibo-demo` などで構いません。
3. Renderの設定はSingaporeなので、Neonでも選べる場合は同じ地域を選ぶと通信が速くなります。
4. Neonの **Connect** からJava/JDBC接続URL、ユーザー名、パスワードを確認します。URLはSSL接続になっていることを確認します。
5. これらの値はメモ帳やチャット、GitHubには貼らず、次のRenderの環境変数画面にだけ入力します。

## 2. Renderでアプリを公開する

1. [Render Dashboard](https://dashboard.render.com/) にサインインします。
2. **New +** から **Blueprint** を選びます。
3. GitHubを接続し、`mile3995/kakeibo-portfolio` を選びます。Blueprintとして `render.yaml` が検出されることを確認します。
4. サービス名 `kakeibo-demo`、地域Singapore、プラン **Free** を確認します。
5. Renderが求める環境変数を設定します。

| 名前 | 入れる値 |
| --- | --- |
| `SPRING_DATASOURCE_URL` | NeonのJava/JDBC URL（`jdbc:postgresql://` で始まるもの。SSL必須） |
| `SPRING_DATASOURCE_USERNAME` | Neonが表示するDBユーザー名 |
| `SPRING_DATASOURCE_PASSWORD` | Neonが表示するDBパスワード |

`APP_DEMO_MODE=true`、HTTPS用Cookie設定、DBスキーマ設定は `render.yaml` に含まれています。接続URLにユーザー名やパスワードが埋め込まれている場合は、そのURLを公開ファイルに保存しないでください。

6. プランがFreeであることを確認して作成・デプロイします。ビルドが完了し、Renderのステータスが **Live** になったら、表示された `https://...onrender.com/login.html` を開きます。
7. 架空のテスト用アカウントを登録し、ログイン画面が開くことを確認します。実際の家計情報は入力しないでください。

## 3. Renderに登録する環境変数

`render.yaml` はWebサービスの設定を用意します。データベース接続用の3項目は、Neonのプロジェクト作成後にRenderの環境変数画面へ入力します。接続情報やパスワードをGitHubやチャットへ送らないでください。

## 公式情報

- [Render Freeプランと制限](https://render.com/docs/free)
- [Render Blueprintの設定](https://render.com/docs/blueprint-spec)
- [NeonのプランとFree枠](https://neon.com/docs/introduction/plans)

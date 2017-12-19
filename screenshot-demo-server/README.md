# screenshot-demo-server

- [sst-devtools-alter-proxy](https://github.com/SecureSkyTechnology/sst-devtools-alter-proxy) にHTTP通信ログをWebで見れる機能 + スクリーンショット画像を添付する機能を追加したローカルHTTPプロキシです。
- Chrome拡張/Firefoxアドオンのサンプルコードである screenshot-demo-extension と連動させて使います。

## requirement

* Java8

## 使い方

URLパスをローカルファイルシステムにマッピングする機能については [sst-devtools-alter-proxy](https://github.com/SecureSkyTechnology/sst-devtools-alter-proxy) の README.md を参照してください。

ローカルHTTPプロキシとして使う場合:
1. jarファイルをDLし、ダブルクリックして起動します。
1. "target host" で HTTP通信ログの保存対象とするホスト名を設定します。
1. "excluded filename extension" で HTTP通信ログ保存対象外とするファイル名の拡張子を設定します。(画像ファイルなど)
1. "proxy port" でproxyとしての待受ポート番号を設定します。
1. Proxyの start / stop ボタンでproxyを起動/停止します。
   - 起動は一瞬ですが、停止は数秒かかります。
1. HTTP通信ログを確認するには、WebUI のstartボタンをクリックします。正常に起動したメッセージボックスでOKをクリックすると、WebUIのアドレスでブラウザが開きます。

HTTPS MITMではCA証明書を毎回ランダムに生成します。そのため、単独で普段使いするには厳しいかもしれません。
Webブラウザには固定のCA証明書を設定/DLできるBurpなどをプロキシに設定し、そのプロキシの上流に設置すると使いやすいでしょう。

興味ある人はいろいろ改造してみてください。ローカルHTTPプロキシは以下のライブラリを使っています。
- Netty : http://netty.io/
- LittleProxy : https://github.com/adamfisk/LittleProxy
  - NettyベースのHTTPプロキシライブラリ
- browsermob-proxy : "MITM with LittleProxy" : https://github.com/lightbody/browsermob-proxy/tree/master/mitm
  - LittleProxy をHTTPSのMITMに対応させるためのプラグイン

### 設定保存と保存先

- proxy起動時、およびアプリ終了時にその時点の設定が保存されます。
- 保存先 : `$HOME/.screenshot-demo-server.yml` 
- HTTP通信ログ/添付画像は保存されません。(興味ある人は改造してみてください)

## 開発環境

* JDK >= 1.8.0_92
* Eclipse >= 4.5.2 (Mars.2 Release), "Eclipse IDE for Java EE Developers" パッケージを使用
* Maven >= 3.5.2 (maven-wrapperにて自動的にDLしてくれます。pom.xml自体は Eclpse 4.5.2 m2e のデフォルト組み込みバージョン : 3.3.3 でも問題なくビルドできます。)
* ソースコードやテキストファイル全般の文字コードはUTF-8を使用

## ビルドと実行

```
cd screenshot-demo-server/

ビルド:
mvnw package

jarファイルから実行:
java -jar target/screenshot-demo-server-(version).jar

Mavenプロジェクトから直接実行:
mvnw exec:java
```

## Eclipseプロジェクト用の設定

https://github.com/SecureSkyTechnology/howto-eclipse-setup の `setup-type1` を使用。README.mdで以下を参照のこと:

* Ecipseのインストール
* Clean Up/Formatter 設定
* 必須プラグイン Lombok / オプションプラグイン Swing Designer のインストール
* GitでcloneしたMavenプロジェクトのインポート 

 Swing Designer 備考:

* 初めてSwing Designerでフレームを作成し、レイアウトで `MigLayout` を選択したところ、Eclipse プロジェクト直下に `miglayout15-swing.jar` と `miglayout-src.zip` が自動でDLされ、Eclipse プロジェクトの Java Build Path にライブラリとして自動で追加されてしまった。
* Swing Designer が掴んでいたためか、Eclipse 起動中はこれらのファイルは完全には削除できなかった。
* →そのため、一旦Eclipseを終了させてファイルを削除したり、Eclipseプロジェクト プロパティのJava Build Path からこれらのjarを手作業で削除したりした。
* さらに、そのままでは `MigLayout` 関連のimportでエラーとなるため、pom.xml に同等の `com.miglayout:miglayout-swing:4.2` を追加してコンパイルエラーを解決した。

`MigLayout` で使用している `miglayout-swing` について(2017-12-18時点):

* http://www.miglayout.com/
* もともと http://www.migcalendar.com/ というJavaのGUIのカレンダーコンポーネントを開発している会社の製品。
* ライセンスとしてはBSD/GPLのデュアルライセンスなので、今回の利用には問題ないと判断した。(2017-12-18)

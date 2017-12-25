# イントラ向けのWebExtensionsの配布方法の調査・検証メモ

業務改善などを目的としたWebExtensionsを作成するとなると、publicなStore (Chrome WebStore, Firefox AMO) 上で公開・配布するのが難しい。
そのような場合、クローズドなイントラ環境で WebExtensions を配布する方法でどのようなアプローチがあるか調査・検証したメモです。

業務改善を目的とした WebExtensions をpublicに公開することによるリスク:
- 業務改善という目的から、WebExtensions内部に業務で使っている社内/社外のサーバ情報などが含まれる可能性が高い。
- WebExtensions自身はローカルにダウンロードされ、JS/HTML/CSSで構成されているため、ソースコード/HTML/CSSの内容をそのまま見ることができてしまう。

上記の前提から、以下のようなリスクが考えられる。
- 公開範囲の設定不備による情報漏えい
  - WebExtensions のpublicにする / private制限しても、制限にミスがあり見えてはならないユーザに見れてインストールされてしまう
  - -> WebExtensions内部のサーバ名・アカウント・APIキーなどの秘密情報が漏洩する。

このリスクを回避または軽減するにはどうするか、の調査・検証メモとなります。

前提:
- 対象組織では G Suite を導入済み。
- WebExtensions としては Chrome拡張 と Firefox Addon を対象とする。(Edgeについては対象外)
- 2017年12月時点の調査結果なので、将来の Chrome Web Store, Firefox AMO の改善により機能や挙動が変わる可能性があります。

## 公式ドキュメント参考

Chrome拡張について:
- "Publish a private Chrome app - Chrome for business and education Help"
  - https://support.google.com/chrome/a/answer/2663860?hl=en
  - これが、作成した Chrome拡張やChromeアプリをWebStore上でprivateに公開するやり方の解説。
- "Create a private Chrome app - Chrome for business and education Help"
  - https://support.google.com/chrome/a/answer/2714278?hl=en
  - こちらは企業専用のbookmark Chromeアプリ(Chromeの「アプリ」メニューで表示され、クリックすると特定サイトが開く、でかいブックマークとしてのアプリ)を作成し、**企業が管理しているWebサーバ上でアプリのパッケージファイルをホスティングして** 、それをPrivateに公開するやり方の説明、のうちの、アプリを作るところの解説。
- "Create a Chrome app collection - Chrome for business and education Help"
  - https://support.google.com/chrome/a/answer/2649489
  - WebStore上で、"for (ドメイン名)" といった、そのG Suiteドメイン専用のコレクションを作成し、そこにprivateアプリを集めてリーチしやすくする方法の説明。

## G Suite の設定

「Chrome Web StoreにChrome拡張をアップロードするための権限」が必要で、それを G Suite で有効化する。

- 「端末管理」-> ("端末の設定"の下の)「Chrome管理」(注意："Chrome搭載端末"ではない) -> 「ユーザー設定」 -> この下に、設定したい組織をクリック -> 「Chrome ウェブストアのアクセス許可」の、以下の項目にチェックを入れる。
  - 「ドメインに制限された限定公開アプリケーションをユーザーが Chrome ウェブストアに公開することを許可する。」（必須）
    - 「ユーザーは、所有していないウェブサイトの確認をスキップできます。」（これは自前のWebサイトでパッケージファイルをホスティングする場合に、必要となるらしい）

## Chrome Web Store の公開設定オプション

デモ用のサンプルChrome拡張を実際にWeb Storeで公開して、公開設定オプションによる挙動を確認してみた。
- https://chrome.google.com/webstore/developer/dashboard

「限定公開」:
- リンクを知っているユーザのみ公開される。
- -> 検索しても表示されない。(G Suite のGoogleアカウントでログインしていても、検索一覧に出てこない)
- -> URLリンク直接アクセスなら、G Suite 以外のメンバーにも誰でも閲覧・インストールできる。(Googleアカウント未ログイン状態でも同じ)

「非公開」 x 「(G Suite ドメイン) のすべてのユーザー」
- 検索しても表示されない。(G Suite のGoogleアカウントでログインしていても、検索一覧に出てこない)
- G Suite ドメイン以外のGoogleアカウント(未ログイン含む)でURL直接アクセスしても、表示されない。
- G Suite のGoogleアカウントでログインしている場合のみ、URL直接アクセスで表示され、インストールできる。

「非公開」 x 「デベロッパーダッシュボードのTrusted Testerのみ」 x 「アクセスを(G Suite ドメイン)のユーザに限定します。」
- 検索しても表示されない。(G SuiteのGoogleアカウント / Trusted Testerに登録したアカウントでログインしていても、検索一覧に出てこない)
- G Suite ドメイン以外のGoogleアカウント(未ログイン含む)でURL直接アクセスしても、表示されない。
- G Suite のGoogleアカウントでログインしても、Trusted Testerに登録していないユーザからは、URL直接アクセスしても表示されない。
- G Suite のGoogleアカウントかつ、Trusted Testerに登録しているユーザからのみ、URL直接アクセスで表示され、インストールできる。
- ※テスターアカウントに自分自身を入れておかないと、登録した自分自身がテストできなくなる。

WebStoreの公開設定オプションから考えられる、リスク対策:
1. 「非公開」 x 「(G Suite ドメイン) のすべてのユーザー」
   - 公開設定で「非公開」を選択することにより、たとえ G Suite アカウントでログインしていても検索に表示されることが無くなる。(URL直接アクセスのみ)
   - -> 直接アクセス用のURLのみ、社員(orその中でも特に限定されたメンバー)しか見えない場所で共有することで、G Suite のメンバーの中でさらに組織で分けて他の組織のメンバーには見せないようなアクセス制限を実装できる。
   - メリット
     - 設定が一番シンプル。
   - デメリット
     - URLがわかってしまうと、G Suite 中の全メンバーがインストールできてしまう。
1. 「非公開」 x 「デベロッパーダッシュボードのTrusted Testerのみ」 x 「アクセスを (G Suite のドメイン) のユーザに限定します。」
   - Chrome拡張を登録した人が dashboard上で設定したTrusted TesterのみがURL直接アクセスでインストールできる。(それ以外のユーザがURLにアクセスしても404)
   - メリット
     - 完全にメンバーを限定できる。URLが漏れても、Trusted Tester以外は404になり、表示・インストールできない。
   - デメリット
     - Trsuted TesterはChrome拡張登録した人ごとに1セットしか登録できない上に、メールを一つずつ手入力するしかないため、これを通常のアクセス制御に使うのは運用面で厳しい。

他：
1. 上記方式と合わせて、サーバ情報など秘密情報はオプション設定としてハードコードしないようにする。万一Chrome拡張が見えてはいけない人に見えてしまっても、設定値が分からなければ使えないような設計にする。
1. WebStoreでの公開ではなく、社員しかアクセスできない/社員しか知らないWebサーバ上にChrome拡張を置いて、更新情報もそこを見に行くようにする。
   - 実態はURLを秘密にする方式とそんなに変わらない。また、後述の通り2017-12時点ではChrome拡張でこれを簡単に実現する方法を調査・検証できていない。
   - Firefoxでは2017-12時点で検証できている + Firefox では G Suite のアクセス制御と連動させることができない ので、Firefox Addon の配布でアクセス制御を運用するには、この方式しか無いだろう。

## イントラ内の独自のWebサーバ上でWebExtensionを配布する方法：Chromeの場合

**結論 : 実現するためのハードルが非常に高く、2017-12-14時点では成功していない。** (Chrome63 on Win10 Pro 64bit)

そもそもWebExtensions + Chrome Web Store が外部に公開することを前提とした仕組みなので、 G Suite のユーザ・グループ・組織構成と連動したアクセス制御の連携が不十分。

さらに、Webサイトを訪問しただけで悪意のあるChrome拡張入れられてしまうリスクに対応するため、独自Webサーバからのインストールについてかなり制限を厳しくしているという印象を感じた。

- 全てを網羅できていないが、以下↓の制約が必要となりそう。
- Chromeの設定となる master_preference json ファイルを修正して強制的にインストールさせる、主にエンタープライズ向けの方式。
  - https://support.google.com/chrome/a/answer/188453
  - http://dev.chromium.org/administrators/policy-list-3#ExtensionInstallForcelist
- Windowsのレジストリで、Chrome拡張をダウンロード・インストール可能なURLのホワイトリストを登録する。
  - https://support.google.com/chrome_webstore/answer/2664769 > "Install an extension from another website"
  - http://www.chromium.org/administrators/policy-list-3#ExtensionInstallSources
- "Inline Installation" : Chrome拡張を配置するWebサーバを、verified (web master ツール経由) な状態にする。(= 外部に公開して、Googleの検索システムに何らかの登録が必要、という考えか)
  - https://developer.chrome.com/webstore/inline_installation
  - https://developer.chrome.com/webstore/inline_installation#verified-site
- 分かりやすい日本語記事参考：
  - https://qiita.com/komasshu/items/f70984b3ecc985e916f3
  - https://blog.hika69.com/blog/2016/05/27/chrome-extension-inline-install/

実際にローカルWebサーバ上からのインストールを試してみたが、うまく行かなかった。(Chrome63 on windows)
- `http://127.0.0.1:3000/updates/we-exercise2.crx` で公開するようにしてみて、chromeでこのURLにアクセスしてみた。
- -> 「アプリ、拡張機能、ユーザースクリプトはこのウェブサイトからは追加できません。」エラーメッセージが表示された。（「OK」ボタンしか表示されず、「危険を承知でインストールしますか？」という選択の余地は無かった）

ローカルWebサーバ上からの"Inline Installation" も試してみたが、駄目だった。
- 参考 : https://developer.chrome.com/webstore/inline_installation
- -> `http://127.0.0.1:3000/updates/install.html` を以下のような内容で用意して、Chromeからアクセスしてみる。
```
<!DOCTYPE html>
<html>
  <head>
    <meta charset="UTF-8">
    <title>we-exercise2 Inline Installation</title>
    <link rel="chrome-webstore-item" href="http://127.0.0.1:3000/updates/we-exercise2.crx">
  </head>
  <body>
    <button onclick="chrome.webstore.install()" id="install-button">Add to Chrome</button>
    <script>
    if (chrome.app.isInstalled) {
        document.getElementById('install-button').style.display = 'none';
    }
    </script>
  </body>
</html>
```
- -> "Add to Chrome" をクリックしたところ、`Uncaught Invalid Chrome Web Store item URL.` というJSエラーが発生した。
- -> **Inline Installを動かすには、Webサイトを Web Master Tool で登録するなどして外部公開する必要がありそう。**
- -> 逆に言えば、Web Master Tool に登録済みのWebサイトがあれば公開自体はすぐできそうだが、そうなると 社内の部署/組織レベルでのアクセス制御実装が難しくなる。URLを推測しづらくして、それで保護するしか無い。
  - 独自のログイン機能などでのセッション制御を入れるのもありだが、実際のChrome側でどうそれが影響するのか未知数すぎる。
- では、Google Drive ならどうか？アクセス制御があるし、あそこならGoogleのお膝元なのでweb master toolの制約はクリアできているのでは？
  - -> 2017年現在、Google Drive のWebホスティング機能が削除されてしまい、動いていないため、使えない。

色々調べてみたが、そもそもChrome側としては「迂闊に変なWebサイト開いて、怪しいChrome拡張インストールされたら危ないから、Chrome拡張を配布できる独自Webサーバには色々制限かけましょうね」というスタンスに見える。

そのため、イントラ内などの独自WebサーバからのChrome拡張配布はかなりハードルが高くなっており、 **粒度の細かいアクセス制御が必須となる業務用Chrome拡張を、カジュアルに独自Webサイト経由から配布できる状況ではないと思われる。**

## イントラ内の独自のWebサーバ上でWebExtensionを配布する方法：Firefoxの場合

**結論：2017-12時点ではChromeと比べて簡単に配布できる。ただし推測しづらいURLによってアクセス制御を実施しなければならないのと、自動更新を正常に動作させる条件が調査しきれていない。**

- AMOの開発者サイトにて、zipパッケージをアップロードして署名済みのXPIを生成してもらう必要がある。
  - https://developer.mozilla.org/en-US/Add-ons/Distribution
  - AMOでアカウント作成の必要あり。
  - 「新しいアドオンを登録」で、「自分自身で。」を選択すれば、レビューをスキップしてすぐに署名済みXPIをダウンロードできる。(AMO側での静的チェックは必要)
  - ※web-extを使えば、API認証キーを発行して、web-ext側で自動でAPIを呼び出し署名することができるらしいが、試してみたらうまく動かなかった。
- 署名済みXPIをローカルからインストールするには
  - https://developer.mozilla.org/en-US/Add-ons/WebExtensions/Alternative_distribution_options/Sideloading_add-ons
  - Firefoxのアドオン画面から、ローカルのXPIファイルをロードしてインストールできる。
  - ただしこの方法だと、自動更新機能が動作しないらしい（未検証）。
- 署名済みXPIをWebサーバからインストールするには
  - 署名済みXPIをWebサーバに配置し、FirefoxでそのURLにアクセスする。
  - こちらは、内部だけの http://127.0.0.1/ のアドレスでもインストールできた。
  - ただし、自動更新機能が動作するかは未検証。
- 自動更新機能について
  - AMOで公開する場合は、以下のメタJSON情報など自動でやってくれるので気にする必要はない。
  - 自前のWebサーバで配布する場合、パッケージバージョンなどのメタ情報を埋め込んだJSONファイルを配置する。
  - https://developer.mozilla.org/en-US/Add-ons/Updates
  - 次に、このJSONファイルのURLを WebExtension 側の manifest.json の以下のキーに埋め込む。この時、URLがhttpsで始まる必要がある。(httpsでどうなるか、自己署名証明書でも動作するか未検証)
```
"applications": {
  "gecko": {
    "update_url": "https://example.com/updates.json"
  }
}
```

- イントラ内などの独自Webサイトから配布すること自体はChrome拡張と比べれば簡単。
- ただし、社内の特定部署に限定するなど粒度の細かいアクセス制御はFirefox側では用意されていないので、URLを推測しづらくするなどで実装するしか無い。
  - Webサイト上で独自のログイン・セッション制御を入れた場合に、どうなるか不明。
- 自動更新機能を有効にするためにはhttpsで提供する必要があるが、自己署名証明書の場合にどうなるか不明。
  - 単純に扱うなら、古いバージョンを一度削除した後、新しいバージョンを入れ直してもらうのが早いが、そうなると storage APIを使った設定値の保持が難しくなる。恐らく Firefox Sync を使えば解決するかもしれないが、それだけのために使うというのも微妙・・・。




# WebExtensions Exercise 2

- [gyazo/gyazo-browser-extension](https://github.com/gyazo/gyazo-browser-extension) の構成を参考にした、シングルソースから Chrome / Firefox / Edge 用のパッケージングを生成するサンプルです。
  - 時間の都合上、Edge用のビルド・パッケージングは見送りました。
- 本サンプルでは以下のプロダクトを活用し、シングルソースからの Chrome / Firefox 用のビルドを実現してみました。
  - [then-chrome](https://www.npmjs.com/package/then-chrome)
    - WebExtensions APIがFirefoxではPromiseを返すのに、ChromeではPromise未対応というばらつきがある。
    - それを解消するため、Chrome の API を Promise でラップできるようにしたライブラリで、これにより Promise の書き方で統一できた。
  - [chrome-browser-object-polyfill](https://www.npmjs.com/package/chrome-browser-object-polyfill)
    - WebExtensions APIの名前空間が Chrome では `chrome`, Firefox/Edge では `browser` となってしまっているのを、どちらでもOKにしてくれる Polyfill
    - これにより、最終的には `chrome` 名前空間で統一してコーディングできた。
  - babel + webpack : ES6/ES7 で WebExtensions を記述できる。
    - WebExtensions に対応したブラウザは事実上、ES6 までほぼ対応しているので、古いブラウザとの互換性はほとんど気にしなくて良かった。
  - [wemf](https://www.npmjs.com/package/wemf) : manifest.json のブラウザごとの細かい互換性を調整してくれる。
    - ただし限界もあり、 `webpack.config.js` でかなり泥臭い調整コードを加える必要があった。
  - [crx](https://www.npmjs.com/package/crx) : Chrome用のパッケージング
  - [web-ext](https://www.npmjs.com/package/web-ext) : Mozilla が開発・提供している Firefox 向けのパッケージングツール
- Windows上のCygwin / Git Bash 上からビルドできるよう、細かい調整・泥臭い調整を随所に入れています。

## サンプルの機能紹介

WebExtensions の主要なUIコンポーネントを background page や storage API で連携させて、簡単なデモ機能を試せるようにしています。

- background page のデモ
  - インストール時に storage に初期設定値を投入
  - background page 内でグローバルなカウント値 / タブ毎のカウント値を保持する。
  - 他のコンポーネントとメッセージングでカウントアップ/現在カウント値の取得/一部設定の保存などを連携する。
  - badge にグローバルなカウント値を表示 + option page からの背景色変更を反映する。
  - webRequest API を使ってHTTPリクエストに独自のカスタムヘッダーを挿入する。
- browser action のデモ
  - popup のHTMLをclickしたら console.log にメッセージ出力。
  - background page にメッセージを送信し、カウントアップを実行。
  - グローバルなカウント値と、タブ毎のカウント値をそれぞれ popup 中に表示。
  - popup から option page / bundled web page (settings.html) を開くデモ。
- option page のデモ
  - browser action の badge の背景色と、background page のグローバルカウント値を編集。
  - 保存時には background page にメッセージを送信することで保存してみた。
- bundled web page (settings.html/js) のデモ
  - `extension.getURL()` でバンドルされた任意のHTMLページを新しいタブで開くデモ。
  - 以下の設定画面を実装。
  - background page : HTTPリクエストに挿入する独自のカスタムヘッダー
    - background page へのメッセージ送信で設定を即時反映。
  - content scripts : HTMLレスポンスのbodyタグの直後に挿入するHTMLコンテンツ
- content scripts のデモ
  - HTMLレスポンスのbodyタグの直後に任意のHTMLコンテンツを挿入。
  - ただし Firefox において store API を使った設定値取得や、background page との連携が正常に動作しなかったため、2017-12時点ではChromeのみの動作検証にとどまっている。

## ビルド, Lint, パッケージング

依存モジュールをローカル(`./node_modules/`)インストール
```
$ npm install
```

ビルド : `./dist/` 以下に各ブラウザごとに調整されたファイルが生成されます。
```
$ npm run build
```

lint : [JavaScript Standard Style](https://standardjs.com/) を使用
```
$ npm run lint
```

パッケージング : `./build/` 以下に `.crx` や `.zip` ファイルを生成
```
$ npm run pack
※clean -> build - > パッケージングがひとまとめに実行されます
```

生成ファイルを削除:
```
$ npm run clean
```

build, dist ディレクトリまるごと削除 : (※生成した鍵ファイルなども削除されますので、本番ビルド環境では非推奨)
```
$ npm run clean:more
```

## 配布の検証

- 配布の検証で、自前のWebサーバ上に配置してインストールや自動更新の動作を検証するためのスクリプトを作っています。
- ただ、結果として、2017-12時点では自前のWebサーバ上からのインストールや自動更新を動かせませんでした。
  - Chrome 63 ではそもそも自前のWebサーバからのインストールを動かせなかった。
  - Firefox 57 では自前のWebサーバからのインストールは動いたが、自動更新までは確認できなかった。
- 参考までに、検証で作成したスクリプトの説明と動かし方を記載しておきます。

`pack-chrome.js` : 自動更新用XMLファイルを生成できるよう、[crx](https://www.npmjs.com/package/crx) をプログラムコードで実行している。
```
$ npm run build
(./dist 以下に生成されていることを前提)

$ node pack-chrome.js
-> ./build/ 以下に 自動更新用のXMLや .zip, .crx ファイルを生成
```

`webpack.config.js` : ビルド時にFirefox用の自動更新用 manifest.json を生成し、 `./build/` 以下に保存しています。

`publish.js` : `./build/` 以下を http://127.0.0.1:3000/updates/ として公開するローカルWebサーバです。
```
$ npm start
```



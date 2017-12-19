# study-webextensions-201712

2017年12月にWebExtensionsの作り方を勉強したときのサンプルコードや調査メモです。

本READMEではローカルのWebExtensionsのインストールやデバッグ方法、動作検証環境についてまとめています。
サンプルコード個別のビルド方法や説明については、それぞれのREADMEを参照してください。

サンプルコードの構成:
- we-exercise1
  - browser actionでpopupを表示させるだけの動作確認用です。
- we-exercise2
  - browser action, background page, option page などを連携させるサンプルです。
- screenshot-demo-extension
  - browser actionで現在アクティブなタブの画面スクリーンショットを取得し、screenshot-demo-serverに送信するサンプルです。
- screenshot-demo-server
  - Nettyで作ったローカルHTTPプロキシで、SpringによるWebUIを組み込んでいます。
  - screenshot-demo-extension から送信されたスクリーンショットを、HTTP通信ログに紐付け、WebUI上に表示します。
- child_process_args_problem
  - Windows環境でNode.jsから子プロセスを起動する場合に、引数がどのように渡るか検証したサンプルです。
- require_json_problem
  - Windows環境でNode.jsからjsonをrequire()した時に、エラーが発生したパターンがあったので検証に用いたサンプルです。

## 動作検証環境

基本的に以下の環境でサンプルコードの動作検証を行いました。
- Win10 Pro (1709) 64bit 日本語版
- Chrome 63 (64bit)
- Firefox 57 (64bit)
- Microsoft Edge 41
- node v8.9.1 (2017-12時点でのLTSを採用)
- npm 5.5.1
- nodist v0.8.8 (nodeのインストールに使用)
- Cygwin (64bit) (コマンドライン操作に使用)
- Git for Windows 2.14.2.2 (Git Bashをコマンドライン操作に使用)

nodistをインストールして再起動すると、マシンの環境変数に以下が追加され、CygwinやGit Bashのmintty環境からもシームレスに使える状態になりました。
```
NODIST_PREFIX=C:\Program Files (x86)\Nodist
NODIST_X64=1
PATH=...;C:\Program Files (x86)\Nodist\bin;...
```

組み合わせによっては `.bashrc` / `.bash_profile` などの調整が必要になるかもしれません。(今回は調整無しで行けたのでラッキーだった)

Cygwin上から問題なく node と npm をインストールできました。
```
$ nodist + 8
$ nodist npm match
$ nodist global 8
$ node -v
v8.9.1
$ npm -v
5.5.1
```

## WebExtensionsの開発情報

Firefox:
- MDNからのWebExtensionsトップページ
  - https://developer.mozilla.org/en-US/Add-ons/WebExtensions
- MDNからのWebExtensionsサンプル集
  - https://github.com/mdn/webextensions-examples

Chrome:
- What are extensions? - Google Chrome
  - https://developer.chrome.com/extensions
- Sample Extensions - Google Chrome
  - https://developer.chrome.com/extensions/samples

Edge: (Windows 10 Anniversary Update 以降)
- Extensions - Microsoft Edge Development | Microsoft Docs
  - https://docs.microsoft.com/en-us/microsoft-edge/extensions
- Extensions for Microsoft Edge - Microsoft Store
  - https://www.microsoft.com/en-us/store/collections/edgeextensions/pc
- Extensions - Porting Chrome extensions - Microsoft Edge Development | Microsoft Docs
  - https://docs.microsoft.com/en-us/microsoft-edge/extensions/guides/porting-chrome-extensions
  - Chrome拡張の既存ソースを、Edge拡張用に変換するツールも出てる。

## ローカルのWebExtensionsをロードする

Firefox57以降:
1. `about:debugging` を開く。
1. "Load Temporary Add-on" (「一時的なアドオンを読み込む」)ボタンをクリック
1. ローカルのWebExtensionsの `manifest.json` を選択
1. -> Firefoxを再起動すれば、ロードしたWebExtensionsはクリアされる。

Chrome:
1. `chrome://extensions` を開く。
1. "Developer mode"(「デベロッパーモード」) にチェックを入れる。
1. "Load unpacked extension..."(「パッケージ化されていない拡張機能を読み込む...」) をクリックし、`manifest.json` が置かれているフォルダを選択
1. -> Chromeの場合は、再起動しても残ってる。

Edge:
1. `about:flags` を開く。
1. "Enable extension developer features"(「拡張機能の開発者向け機能を有効にする」) にチェックを入れる。(ブラウザ再起動が必要)
1. ブラウザ再起動後、"More (...)" メニュー -> "Extensions"(「拡張機能」) を開く
1. "Load extension"(「拡張機能の読み込み」)をクリックし、`manifest.json` が置かれているフォルダを選択
1. -> Edgeの場合、再起動すると「不明な発行元からの拡張機能を無効にしました。～」通知が表示されることがある。デフォルトは無効になってるので、「有効にする」をクリックする必要がある。
   - 後で、拡張機能メニューから個別に拡張を有効/無効切り替えられる。

## WebExtensionsのデバッグ方法

Firefox: https://developer.mozilla.org/en-US/Add-ons/WebExtensions/Debugging
- `about:debugging` を開き、"Enable add-on debugging"(「アドオンのデバッグを有効化」)にチェックを入れる。
- 一時的に拡張をロードして、「デバッグ」をクリック -> 接続確認のダイアログが表示されたら"OK"をクリック。
  -  background page / content script / option page / popup などのデバッグが可能。(詳細はMDN参照)
  -  poup については、Developer Tool の「ポップアップが自動的に隠れないようにします」を有効化しないとうまくデバッグできない。

Chrome: https://developer.chrome.com/extensions/tut_debugging
- `chrome://extensions/` を開き、デベロッパーモードにチェックを入れる。
- browserActionに対応してる = アドレスバーの右にアイコンが表示されるタイプの拡張なら、アイコンを右クリックして「ポップアップを検証」をクリックするとその拡張用のDeveloper Toolsが表示される。
- バックグラウンドページを利用している拡張なら、 `chrome://extensions/` の拡張一覧で「ビューを検証：バックグラウンドページ」リンクが表示されるので、リンクをクリックするとその拡張用のDeveloper Toolsが表示される。

Edge: https://docs.microsoft.com/en-us/microsoft-edge/extensions/guides/debugging-extensions
- background page / content script / popup page それぞれでDeveloper Toolsを開いてデバッグできる。

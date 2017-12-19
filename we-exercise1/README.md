# WebExtensions Exercise 1

- WebExtensions版 "hello, world" です。
- browser action でデモ用の popup.html を表示します。
- popup.js中の console.log() で "hello" を出力しています。
- Chromeで "hello" を確認 : browser actionのアイコンを右クリックして「ポップアップを検証」をクリックします。
- Firefoxで "hello" を確認 : `about:debugging` で当該アドオンの「デバッグ」を実行し、開発ツールが起動したら browser actionをクリックしてポップアップを表示させます。
  - 開発ツールの歯車アイコンから、「ポップアップが自動的に隠れないようにします」にチェックを入れたほうが良いかも。(ポップアップのHTMLから離れた途端に、開発ツールのコンソールがpopup.jsから切り替わってしまい、見えなくなってしまう。)

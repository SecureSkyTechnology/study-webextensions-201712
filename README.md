# study-webextensions-201712

2017年12月にWebExtensionsの作り方を勉強したときのサンプルコードや調査メモです。

サンプルコードの構成:
- we-exercise1
  - browserActionでpopupを表示させるだけの動作確認用です。
- we-exercise2
  - browserAction, background page, option page などを連携させるサンプルです。
- screenshot-demo-extension
  - browserActionで現在アクティブなタブの画面スクリーンショットを取得し、screenshot-demo-serverに送信するサンプルです。
- screenshot-demo-server
  - Nettyで作ったローカルHTTPプロキシで、SpringによるWebUIを組み込んでいます。
  - screenshot-demo-extension から送信されたスクリーンショットを、HTTP通信ログに紐付け、WebUI上に表示します。
- child_process_args_problem
  - Windows環境でNode.jsから子プロセスを起動する場合に、引数がどのように渡るか検証したサンプルです。
- require_json_problem
  - Windows環境でNode.jsからjsonをrequire()した時に、エラーが発生したパターンがあったので検証に用いたサンプルです。




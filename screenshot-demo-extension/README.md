# screenshot-demo-extension

- WebExtensions の `tabs.captureVisibleTab()` APIを使ったスクリーンショット(画面キャプチャ)のサンプルです。
- browser action の click イベントで、現在のタブの画面キャプチャを取得し、アドレスバーのURLと一緒に screenshot-demo-server にアップロードします。
- デフォルトと異なるアドレスで screenshot-demo-server を実行する場合は、option page でアップロード先URLを変更してください。

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

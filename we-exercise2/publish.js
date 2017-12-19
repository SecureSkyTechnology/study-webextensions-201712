/**
 * ローカルWebサーバ上で更新パッケージ(.crx/.xpi)を配布するための、静的Webサーバ起動スクリプト。
 * ローカルの "./build" ディレクトリを "/updates" path にマッピングして公開します。
 */
const path = require('path')
const express = require('express')
const serveIndex = require('serve-index')
const app = express()
const port = 3000

const localdir = path.join(__dirname, 'build')
app.use(
  '/updates',
  express.static(localdir, {
    setHeaders: (res, path, stat) => {
      const ext = path.split('.').pop()
      if (ext === 'crx') {
        res.set('Content-Type', 'application/octet-stream')
      }
    }
  }),
  serveIndex(localdir, {'icons': true})
)

app.listen(port, () => console.log(`Firefox and Chrome extension hosting server launched at http://127.0.0.1:${port}/updates`))

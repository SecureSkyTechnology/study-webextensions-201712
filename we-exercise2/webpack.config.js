const path = require('path')
const fs = require('fs')
const CopyWebpackPlugin = require('copy-webpack-plugin')
const WebpackOnBuildPlugin = require('on-build-webpack')
const exec = require('child_process').execSync
const mkdirp = require('mkdirp')

const distPath = 'dist'
// HTML/CSS/BabelでtranspilerされたJS/雛形manifest.json がまず 'dist/common/' 以下に出力される。
const distPathCommon = distPath + '/common'
// common に出力されたのを各ブラウザ用にコピーして、wemf で manifest.json を調整したりパッケージングする。
const distPathChrome = distPath + '/chrome'
const distPathFirefox = distPath + '/firefox'
const distPathEdge = distPath + '/edge'

let plugins = [
  // src/ 以下のHTML/CSS/画像アセットを dist/common/ にコピーする。(コピー先ルートディレクトリは後述の module.exports の output で指定している)
  // https://www.npmjs.com/package/copy-webpack-plugin
  new CopyWebpackPlugin([
        {from: './src/browser_action/*.png', to: 'browser_action/[name].[ext]'},
        {from: `./src/icons`, to: 'icons'},
        {from: './src/options.html'},
        {from: './src/popup.html'},
        {from: './src/settings.html'},
        {from: './src/manifest.json'}
  ]),
  // webpackのビルド後に、dist/common を各ブラウザ用にコピーして、wemfコマンドでそれぞれのブラウザ用にmanifest.jsonを調整する。
  // https://www.npmjs.com/package/on-build-webpack
  // https://www.npmjs.com/package/wemf
  new WebpackOnBuildPlugin(() => {
    /* https://github.com/gyazo/gyazo-browser-extension/blob/v2.6.1/webpack.config.js
      * 元のソースでは './node_modules/.bin/wemf -U' だが、Cygwin や cmd.exe のWindows上では以下のエラーが発生してしまう。
      * --------------------------------------------------------------
      * '.' は、内部コマンドまたは外部コマンド、
      * 操作可能なプログラムまたはバッチ ファイルとして認識されていません。
      * child_process.js:644
      *      throw err;
      * --------------------------------------------------------------
      * (英語なら ""'.' is not recognized as an internal or external command, operable program or batch file."")
      *
      * '.\\node_modules\\.bin\\wemf -U' にすれば動くが、明らかにこちらだと unix 系で動かなくなる。
      * そこで長谷川さんが見つけた解決策が __dirname を使う方法で、cygwinではこれで成功した。
      * (unix系では未検証だが、原理的に恐らく動くのでは？)
      */
    const wemfCommand = `${__dirname}/node_modules/.bin/wemf -U`
    const extensionBaseName = 'WebExtension Exercise No.2'
    const extensionId = 'sakamoto@securesky-tech.com'
    const firefoxUpdateJsonFile = 'firefox-update-manifest.json'
    /* https://github.com/gyazo/gyazo-browser-extension/blob/v2.6.1/webpack.config.js
      * こちらでは以下の書き方になっているが、Cygwin上のcpコマンドだと
      * "cp: 宛先の 'dist/chrome' はディレクトリではありません"
      * となってしまう。
      * exec(`cp -R ${distPathCommon}/* ${distPathChrome}`)
      * exec(`cp -R ${distPathCommon}/* ${distPathFirefox}`)
      * exec(`cp -R ${distPathCommon}/* ${distPathEdge}`)
      * -> "/*" を削っると、 dist/chrome/common/xxx.js みたいになってしまうのでこれもNG.
      * -> もともと各ブラウザ用のmkdirが無かったので、mkdirp.sync()による作成を追加しました。
      * ()
      */
    mkdirp.sync(distPathChrome)
    mkdirp.sync(distPathFirefox)
    mkdirp.sync(distPathEdge)
    exec(`cp -R ${distPathCommon}/*  ${distPathChrome}`)
    exec(`cp -R ${distPathCommon}/*  ${distPathFirefox}`)
    exec(`cp -R ${distPathCommon}/*  ${distPathEdge}`)

    // execSync() 用オプションで、Cygwin上では以下の設定をしないとwarningなどが表示されない。
    // see https://nodejs.org/api/child_process.html#child_process_child_process_execsync_command_options
    const execOpts = {stdio: 'inherit'}

    /* オリジナルのソースでは `... --data '${JSON.stringify({name: extensionBaseName})}'` という風に処理している。
      * しかし Windows 上でビルドする場合、" や ' のエスケープ及びそれで囲った文字列を正しく最終的なプロセスの引数に渡すのが非常に難しい。
      * というのも、child_process の内部で " を削ったり空白文字で勝手に分割するなど、自動調整による副作用があまりにも大きいため。
      * execSync()ではなくexecFileSync()/spawnSync()では引数をArrayで渡すこともできるが、それで試しても予想できないエスケープが発生したりする。
      * (特に JSON の値の中に " / ' / \\ が入ってくる場合など)
      * そのため、有限時間内でどのプラットフォームでも正確に "--data" オプションの引数で manifest.json の追加内容を指定するのは、断念した。
      *
      * 代替案として、非常にべた書きな形だが、プログラムコードでmanifest.jsonを読み込んで手動でカスタマイズすることにした。
      */

    // Firefox用の調整
    let srcjsonstr = fs.readFileSync(`${distPathFirefox}/manifest.json`, {encoding: 'utf-8'})
    let srcjsonobj = JSON.parse(srcjsonstr)
    srcjsonobj.name = extensionBaseName + ' for Firefox'
    srcjsonobj.applications = {
      gecko: {
        id: extensionId,
        /* see https://developer.mozilla.org/en-US/Add-ons/WebExtensions/manifest.json/applications
         * こちら参考にupdate_urlを設定してみた例。
         * とりあえずAMOでの署名を通すためだけにhttpsにしただけで、実際にhttpsの自己署名でどうなるかは未検証。
         */
        update_url: 'https://127.0.0.1:3000/updates/' + firefoxUpdateJsonFile
      }
    }
    let destjsonstr = JSON.stringify(srcjsonobj)
    fs.writeFileSync(`${distPathFirefox}/manifest.json`, destjsonstr, {encoding: 'utf-8'})
    exec(`${wemfCommand} --browser firefox ${distPathFirefox}/manifest.json`, execOpts)

    // Chrome用の調整
    srcjsonstr = fs.readFileSync(`${distPathChrome}/manifest.json`, {encoding: 'utf-8'})
    srcjsonobj = JSON.parse(srcjsonstr)
    srcjsonobj.name = extensionBaseName
    delete srcjsonobj.options_ui.browser_style // 残っているとChrome63では警告が表示されてしまう。wemfでは除去してくれなかった。
    destjsonstr = JSON.stringify(srcjsonobj)
    fs.writeFileSync(`${distPathChrome}/manifest.json`, destjsonstr, {encoding: 'utf-8'})
    exec(`${wemfCommand} --browser chrome ${distPathChrome}/manifest.json`, execOpts)

    // Edge用の調整
    srcjsonstr = fs.readFileSync(`${distPathEdge}/manifest.json`, {encoding: 'utf-8'})
    srcjsonobj = JSON.parse(srcjsonstr)
    srcjsonobj.name = extensionBaseName + ' for Edge'
    destjsonstr = JSON.stringify(srcjsonobj)
    fs.writeFileSync(`${distPathEdge}/manifest.json`, destjsonstr, {encoding: 'utf-8'})
    exec(`${wemfCommand} --browser edge ${distPathEdge}/manifest.json`, execOpts)

    // こちらもオリジナルソースでは特にbuildディレクトリのmkdirを呼んでいないが、cygwin上での実行を考慮して念のため手動で作成しておく。
    const buildPath = 'build'
    mkdirp.sync(buildPath)

    // 結局動かなかったが、inline installation 用のHTMLを作ってみたので、publish.jsで公開されるbuild/ディレクトリ以下にコピーする。
    exec(`cp chrome-inline-install.html ${buildPath}/`)

    /* Firefox用のupdate メタデータ JSON (see: https://developer.mozilla.org/en-US/Add-ons/Updates)
     * こちら、結局このメタデータのURLを manifest.json : applications.gecko.update_url にhttpsで記載する必要がある。
     * が、自己署名でhttpsを立ててもどうなるか未知数であり、しかもローカルのXPIをインストール(Sideloading)した場合は自動更新が動かないという記述もある。
     * https://developer.mozilla.org/en-US/Add-ons/WebExtensions/Alternative_distribution_options/Sideloading_add-ons
     * そのため、実際にこのメタデータ JSON が動作するかは不明。
     * また、バージョンごとにエントリを追加する + XPIファイル名を埋め込む必要があるが、web-ext sign コマンドが2017-12-14時点でうまく動作せず、
     * ビルドスクリプト中でのXPI生成を実現できなかった。そのため、XPIファイル名がここの地点では正確に予期できない。
     * またバージョンごとにエントリを追加していく形なので、うまく自動化する方法が不明。
     * 2017-12-14時点では、検証の証跡として以下の生成コードのみ残しておくが、これは動作しない点に注意のこと。
     */
    const firefoxUpdateJson = { addons: {} }
    firefoxUpdateJson.addons[extensionId] = {
      updates: [
        {
          version: '1.0',
          update_link: 'http://127.0.0.1:3000/updates/give-up-very-sad...-1.0.xpi'
        }
      ]
    }
    destjsonstr = JSON.stringify(firefoxUpdateJson)
    fs.writeFileSync(`${buildPath}/${firefoxUpdateJsonFile}`, destjsonstr, {encoding: 'utf-8'})

    /* オリジナルソースではchrome用の crx keygen コマンドを package.json の npm scripts から呼んでいるが、
      * unix shell の if 構文で key.pem ファイルの存在判定をしており、Cygwin上では正常に動作しない。
      * "crx keygen" では --force をつけると既存のkey.pemを上書きするが、かといってこのオプション無しだと、
      * 既にある場合にエラーになってしまう。
      * そのため、「無ければ作成するが、あれば上書きせずにそのまま」というのを "crx keygen" 単体で実現できない。
      * このためオリジナルソースでは package.json でわざわざ unix shell の if 構文で制御していたものと思われる。
      *
      * やむを得ず、webpack側のJSコードで同等の機能を実現することにした。
      */
    const crxCommand = `${__dirname}/node_modules/.bin/crx`
    try {
      fs.accessSync(`${buildPath}/key.pem`, fs.constants.R_OK)
    } catch (ignore) {
      exec(`${crxCommand} keygen ${buildPath}`)
    }
  })
]

module.exports = {
  // 生成されたJSにインラインでsource-mapを埋め込む。
  // https://webpack.js.org/configuration/devtool/
  devtool: 'inline-source-map',
  entry: {
    'content-scripts': ['chrome-browser-object-polyfill', 'babel-polyfill', './src/content-scripts.js'],
    settings: ['chrome-browser-object-polyfill', 'babel-polyfill', './src/settings.js'],
    background: ['chrome-browser-object-polyfill', 'babel-polyfill', './src/background.js'],
    options: ['chrome-browser-object-polyfill', 'babel-polyfill', './src/options.js'],
    popup: ['chrome-browser-object-polyfill', 'babel-polyfill', './src/popup.js']
  },
  output: {
    filename: '[name].js',
    // このpath設定が copy-webpack-plugin でのコピー先ルートディレクトリとして使われる。
    path: path.resolve(__dirname, distPathCommon)
  },
  module: {
    loaders: [
      {
        test: /\.js$/,
        exclude: /(node_modules)/,
        loader: 'babel-loader'
      }
    ]
  },
  plugins
}

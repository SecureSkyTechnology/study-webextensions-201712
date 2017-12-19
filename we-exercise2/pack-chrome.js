const fs = require('fs')
const exec = require('child_process').execSync
const ChromeExtension = require('crx')

const buildPath = 'build' // webpack.config.js とうまく共用できないのがイケてない

/* オリジナルソースではchrome用の crx keygen コマンドを package.json の npm scripts から呼んでいるが、
  * unix shell の if 構文で key.pem ファイルの存在判定をしており、Cygwin上では正常に動作しない。
  * "crx keygen" では --force をつけると既存のkey.pemを上書きするが、かといってこのオプション無しだと、
  * 既にある場合にエラーになってしまう。
  * そのため、「無ければ作成するが、あれば上書きせずにそのまま」というのを "crx keygen" 単体で実現できない。
  * このためオリジナルソースでは package.json でわざわざ unix shell の if 構文で制御していたものと思われる。
  *
  * やむを得ず、webpack側のJSコードで同等の機能を実現することにした・・・が、後述のコードにあるように、
  * self-hosting + 自動更新の挙動実験用にupdate用のメタ情報XMLも出力するようにしてみた。
  * これは、練習時点のcrxではコマンドラインでは実装されておらず、コードで書く必要があったため、
  * もう諦めて webpack から追い出して、単体スクリプトに分離した。
  */
const crxCommand = `${__dirname}/node_modules/.bin/crx`
const keyfile = `${buildPath}/key.pem`
try {
  fs.accessSync(keyfile, fs.constants.R_OK)
} catch (ignore) {
  exec(`${crxCommand} keygen ${buildPath}`)
}

// https://www.npmjs.com/package/crx そのまま。
const crx = new ChromeExtension({
  codebase: 'http://localhost:3000/updates/we-exercise2.crx',
  privateKey: fs.readFileSync(keyfile)
})

// see: https://github.com/oncletom/crx/issues/73
function monkeyPatchedResolve (path) {
  return new Promise(function (resolve, reject) {
    return resolve({
      path: path,
      src: '**'
    })
  })
}

crx.load = (path) => {
  var selfie = crx
  return monkeyPatchedResolve(path || selfie.rootDirectory)
    .then(function (metadata) {
      selfie.path = metadata.path
      selfie.src = metadata.src
      selfie.manifest = require(selfie.path + '/manifest.json')
      selfie.loaded = true
      return selfie
    })
}

crx.load('./dist/chrome')
  .then(() => {
    return crx.loadContents()
  })
  .then((zipBuffer) => {
    fs.writeFile(`${buildPath}/we-exercise2.chrome.zip`)
    return crx.pack(zipBuffer)
  })
  .then(crxBuffer => {
    const updateXML = crx.generateUpdateXML()
    fs.writeFile(`${buildPath}/chrome-update.xml`, updateXML)
    fs.writeFile(`${buildPath}/we-exercise2.crx`, crxBuffer)
    console.log('chrome packaging done.')
  })

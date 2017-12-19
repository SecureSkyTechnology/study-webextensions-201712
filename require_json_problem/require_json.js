const path = require('path')
const assert = require('assert')

/**
 * path.join()でのパス解決が、Windows上でうまく行かなくて、
 * その影響でjsonのrequire()が失敗することがあったので、
 * 動作検証用のテストスクリプトを作成してみた。
 * 結果としては、うまく失敗状況を再現できなかった（path.join()パターンがどれも正常に動いてしまう）。
 * もしかしたら、path.join()するのが __dirname なのが原因かもしれない。
 * 失敗していたスクリプトでは、別途フルパス指定が入るところなので、そちらだとまた変わるかも？？？
 * 
 * 2017-12-14時点での動作確認Node Version:
 * v8.9.1 / v9.3.0 (64bit) いずれも Win10 Pro(64)上で同じ挙動となった。
 */

const nver = process.version
const plat = process.platform
const isWindows = (plat === 'win32')
console.log(`===== Node.js : ${nver} / Platform : ${plat} / is Windows ? ${isWindows}`)

function rtest(testno, jsonpath) {
  try {
    const r = require(jsonpath)
    assert(r.hasOwnProperty('message'))
  } catch (e) {
    console.log(`Test No.${testno}-----------------------------------`)
    console.log(`require(${jsonpath}) failed.`)
    console.error(e)
  }
}

rtest(1, 'hello.json') // "."無しだと、require()は相対パスとして扱わず、モジュールロードに失敗する。
rtest(2, './hello.json') // これは成功する。
rtest(3, `${__dirname}/hello.json`)
if (isWindows) {
  rtest(30, `${__dirname}\\hello.json`)
}

rtest(4, path.join(__dirname, 'hello.json'))
rtest(41, path.join(__dirname, '/hello.json'))
rtest(42, path.join(__dirname, '/./hello.json'))
if (isWindows) {
  rtest(43, path.join(__dirname, '\\hello.json'))
  rtest(44, path.join(__dirname, '\\.\\hello.json'))
}

rtest(5, 'foo/bar/baz.json') // "."無しだと、require()は相対パスとして扱わず、モジュールロードに失敗する。
rtest(6, './foo/bar/baz.json') // これは成功する。
rtest(7, `${__dirname}/foo/bar/baz.json`)
if (isWindows) {
  rtest(70, `${__dirname}\\foo\\bar\\baz.json`)
}
rtest(8, path.join(__dirname, 'foo/bar/baz.json'))


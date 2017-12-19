'use strict'

// import thenChrome from 'then-chrome'
import Preference from './lib/Preference'

async function doInsertHTML () {
  const pref = await Preference.get()
  console.log(pref) // これがFirefoxでは出力されない・・・
  const wrapper = document.createElement('div')
  wrapper.innerHTML = pref.insertHTML
  const bodyE = document.getElementsByTagName('body')[0]
  const firstChild = bodyE.firstChild
  bodyE.insertBefore(wrapper, firstChild)
}

// 以下、async/awaitを使わずにpromiseを生で使ってみると・・・
// thenChrome.storage.local.get().then((pref) => {
//     // Chromeの場合はここで正常にstorageの中身がconsoleに表示される。
//     console.log(pref);
// }, (e) => {
//     /* Firefoxの場合、ここで以下のエラーがconsoleに出力される。
//     * InternalError
//     * columnNumber: 0
//     * fileName: ""
//     * lineNumber: 0
//     * message: "Promise rejection value is a non-unwrappable cross-compartment wrapper."
//     * stack: ""
//     */
//     console.log(e);
// });

// // storageは諦めて、backgroundページとのやり取りを試みる。
// const p1 = thenChrome.runtime.sendMessage({command: 'get'});
// // Chromeでは Promise{<pending>} が返されるが、Firefoxでは Promise{<rejected>} が返される。
// // reason, および then() -> (e) での内容は以下となる。
// // message: "Promise rejection value is a non-unwrappable cross-compartment wrapper."
// console.log(p1);
// p1.then(
//     (response) => {console.log(response);},
//     (e) => {console.log(e);}
// );

// 以下も試してみたが、chromeならbackgroundからのresponseがconsoleに表示されるが、
// Firefoxではconsoleに何も(エラーすらも)表示されなくなってしまう。
// 末尾の "started..." しら表示されない。
// const p2 = browser.runtime.sendMessage({command: 'get'}, (r) => {console.log(r);});
// console.log(p2); // chromeなら undefined になる。

// 以上より、全般的に Firefox で Content Script 中からstorage/messageのやり取りを正常に行う方法が見当たらない。
// ググっても、類似事例が見当たらない。
// もしかしたら manifest.json の設定が足りていない可能性もあるし、
// babelを通したJSではなく、本当に素のJSで記述すれば動くかもしれない。
// しかし現時点では相当ハードルが高いため、Firefoxでのデモは断念する。

doInsertHTML()
console.log('started content-scripts.js')

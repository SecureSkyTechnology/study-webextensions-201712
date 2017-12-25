# 今回の調査・検証で困ったところ・嵌ったところ

今回の調査・検証で、筆者(坂本)が遭遇したトラブルや細かいTIPSなどのメモです。
WebExtensionsそのものの他にも、Windows上のNode.jsだったり、 `screenshot-demo-server` での Netty/LittleProxy などの話題も含んでいます。

## ブラウザごとの互換性

ブラウザ毎のWebExtensions API対応状況:
- Browser support for JavaScript APIs - Mozilla | MDN
  - https://developer.mozilla.org/en-US/Add-ons/WebExtensions/Browser_support_for_JavaScript_APIs

他、気がついた差異：
- manifest.json で使えるキーで対応度が違う。
  - ChromeではFirefoxで対応してる "commands" キーが未対応。
  - "sidebar_action" などFirefoxでのみ対応してる機能なども、インストールはできるが動作しない。
  - "applications" キーもFirefoxのみ対応してて、Chromeではインストールはできるが警告が表示される。
  - Edgeでは "author" キーが必須。
- Chromeでは "browser" オブジェクトが未定義のため、FirefoxのWebExtensionsのサンプルがほとんど動作しなかった。
  - 逆に、Firefoxでの "browser" オブジェクトが Chrome では "chrome" オブジェクトに相当する。
  - これについては [chrome-browser-object-polyfill](https://www.npmjs.com/package/chrome-browser-object-polyfill) を使ってカバーできた。
- `icons` に使えるサイズが、Firefoxでは48/96 <> Chromeでは128が必須で48/16と微妙な仕様差がある。
- `browser_action` での `default_icon` に使えるサイズが, Firefox/Chromeでは16/32が推奨、Edgeだと20px, 25px, 30px, 40px とサイズがまた微妙に違う。

## background-page と option の連携でのトラブルメモ

やりたい事：optionページを開いたときに、backgroundページにメッセージを送って、storage.local.get() の結果をレスポンスとして取得したい。
- この意図としては、optionページから設定する値の初期値をstorage.localに保存するのはインストール/アップデートイベントを拾えるbackgroundページが適切に思われ、よってそれらを扱う機能はbackgroundページ側に集約したほうが良さそうに思えたため。
- あるいは、storage.local.get()を拾うときのキー値をbackgroundページのJSのプロパティとして保持するのがglobalっぽくて嫌だったため、キー値を保たずに、メッセージだけでやり取りしたかった。

問題が発生したコード(抜粋):
- background.js
```javascript
async function getPref() {
    return await thenChrome.storage.local.get();
}

chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
// ...
    switch (message.command) {
// ...
        case 'getPref':
            getPref().then((item) => {
                console.log(item);
                sendResponse({
                    badgeBGColor: item.badgeBGColor,
                    globalCount: globalCounter
                })
            })
            break;
    }
});
```
- options.js
```javascript
chrome.runtime.sendMessage({command: 'getPref'}, (response) => {
    console.log(response);
    console.log(chrome.runtime.lastError);
});
```

発生した問題：
- background.js側では getPref()中のconsole.log()にちゃんと、storage.local.set()した値が表示されている。
- しかし、options.js側では response オブジェクトがundefinedになり、chrome.runtime.lastError が以下になってしまう。
```
message: "The message port closed before a response was received."
```

同類と思われるQA:
- https://stackoverflow.com/questions/44056271/chrome-runtime-onmessage-response-with-async-await

- なんとなく、getPref().then()の後に即breakしてるのでそこでmessageのport(雰囲気的な呼び方です)が閉じてしまい、その後 getProf()のthen()のpromiseの中でsendResponse()してるので、その辺が問題な気がしている。options.js側からすれば、即座にport(雰囲気的な呼び方です)がクローズしてしまったためエラーになったのでは？
- 他のonMessage内のcaseでは sendResponse() の呼び出しが非同期処理になっている箇所は無い。
- 今の時点での推測だが、onMessage()のevent listener内で sendResponse() する場合は、そのスコープ内で返さないと駄目なのかもしれない。中で非同期処理呼び出してその中で呼んだらアウト、的な。

この問題をどうにか async/await を駆使して回避しようとしたけど、まだ雰囲気で使ってるレベルなので解決できそうにない。ということで、諦めた。

どうするかだが、結局のところやりたい事は、storage.local(本番プロダクトならsyncのほうが良いだろうけど)にpreferenceを置いておきたくて、その初期化・getter/setterを特定クラスなりJSに集約しておきたい、その最初の候補がbackground.jsだった、ということ。

これが断念された今、それなら単純に、Preference.js クラスのstaticメンバーに入れればいっか、ということで以下のコードにしてみたらあっさり動いた。

- lib/Preference.js(抜粋):
```javascript
'use strict';
import thenChrome from 'then-chrome';
export default class Preference {
    static init() {
        chrome.storage.local.set({
            badgeBGColor: '#FF0000'
        });
    }
    static async get() {
        return await thenChrome.storage.local.get();
    }
}
```
- background.js(抜粋):
```javascript
//...
import Preference from './lib/Preference';
//...
chrome.runtime.onInstalled.addListener((details) => {
    if (details.reason === 'install'){
        Preference.init();
    }
});
```
- options.js(抜粋):
```javascript
async function init() {
    const pref = await Preference.get();
    console.log(pref);
    // ...
    console.log('initialized options.js');
}

init();
```

## lintエラー時の動き

- standard を追加して、lintが動くようにした : https://www.npmjs.com/package/standard
- ただ、Windowsだとstandardのlintでエラーがあると、コマンドの戻り値の @%ERRORLEVEL%=1@ となってnpm scriptの実行それ自体がエラーで終わってしまう。
- -> まるで npm scripts の実行それ自体がエラーとなったように見えてしまい、最初何が起こったのかと理解できなかった。
- npm scriptsは、少なくともWindows上では、コマンドのerrnoが0以外だと実行自体がエラーになったような見え方になるようだ・・・。
- (standardの処理自体は正常で、エラー情報もきちんと表示される）

## web-extでの署名に失敗

npmでMDNが提供しているweb-extでWebExtensionsの署名を試みたが、うまく行かなかった。
- https://www.npmjs.com/package/web-ext

Windows + Cygwin上での実行という組み合わせの問題かもしれない・・・詳細切り分けまで至らず、どこでエラーとなっていたのかも不明なまま時間切れとなった。

1. AMO上で開発者登録して、APIキーを取得。
   - https://addons.mozilla.org/ja/developers/
2. web-extを実行するシェル上で環境変数設定
```
export WEB_EXT_API_KEY=user:(...)
export WEB_EXT_API_SECRET=(...)
```
3. web-ext実行
```
$ ./node_modules/.bin/web-ext sign -s dist/firefox -a build
Building web extension from C:\(...)\study-webextensions-201712\we-exercise2\dist\firefox
Server response: Version already exists. (status: 409)
FAIL

WebExtError: The WebExtension could not be signed
    at _callee$ (C:\(...)\study-webextensions-201712\we-exercise2\node_modules\web-ext\dist\webpack:\src\cmd\sign.js:136:15)
    at tryCatch (C:\(...)\study-webextensions-201712\we-exercise2\node_modules\web-ext\node_modules\regenerator-runtime\runtime.js:65:40)
    at Generator.invoke [as _invoke] (C:\(...)\study-webextensions-201712\we-exercise2\node_modules\web-ext\node_modules\regenerator-runtime\runtime.js:303:22)
    at Generator.prototype.(anonymous function) [as next] (C:\(...)\study-webextensions-201712\we-exercise2\node_modules\web-ext\node_modules\regenerator-runtime\runtime.js:117:21)
    at step (C:\(...)\study-webextensions-201712\we-exercise2\node_modules\web-ext\dist\webpack:\node_modules\babel-runtime\helpers\asyncToGenerator.js:17:1)
    at C:\(...)\study-webextensions-201712\we-exercise2\node_modules\web-ext\dist\webpack:\node_modules\babel-runtime\helpers\asyncToGenerator.js:28:1
    at <anonymous>
    at process._tickCallback (internal/process/next_tick.js:188:7)
```
4. `Version already exists.` となり、署名できなかった。AMO上から、当該拡張自体削除していて空っぽの筈なのだが・・・。

## Netty + LittleProxy で HTTP リクエスト/レスポンス全体を取得するには

Netty + LittleProxy で、実際にProxyとしてHTTPリクエスト/レスポンス全体を記録するにはどうするかちょっと試行錯誤が必要だった。

参考1:
- https://stackoverflow.com/questions/29230268/netty-convert-httprequest-to-bytebuf
- -> FullHttpRequest/Responseを`byte[]`に戻す、あるいはその逆については、要するにEncoder/Decoderをインメモリで動かせれば良いので、EmbeddedChannelを使う、という発想。
```
EmbeddedChannel ch = new EmbeddedChannel(new HttpRequestEncoder());
ch.writeOutbound(msg);
ByteBuf encoded = ch.readOutbound();
```

実際にこれでやってみたところ、HTTPヘッダーの並び順については無難に保持されている。(`byte[]` -> FullHttpRequest/Response -> `byte[]` に戻した時ヘッダーの並び順は維持)
だが、どうもNetty同梱のhttp codecだと、HTTPのJava表現(FullHttpRequest, FullHttpResponse)で以下のような自動調整が入る。
- GETで `Content-Length: 0` がJava表現になると自動でくっつく。
- 複数行ヘッダがJava表現になると自動で連結さえる。
- ヘッダー末尾の空白がJava表現になるとtrimされる。

HTTPリクエスト/レスポンスの生データに触る仕組みはLittleProxy自体には仕込まれていない。
そのため、EmbeddecChannelを使って手動で FullHttpRequest/Response を `byte[]` に変換しようとすると、上記の調整が入ったデータとなり、本当に送られたリクエスト/レスポンスとは若干異なったものになってしまう。

参考2
- -> これの解決ヒントとして、以下のサンプルコードではchannelのpipelineに実際の生 `byte[]` を記録するサンプルが仕込まれている。
  - https://github.com/anjackson/warc-proxy
- ただこのサンプルも動かしていないので、実際にどこで `byte[]` 記録を区切るのか、どうやって取り出すのかなど不明点が多い。
- とはいえ、本当にクライアントが送ったリクエストそのまま / サーバからのレスポンスそのまま をログとして保存するには、将来的にはこのような技法を組み合わせる必要が出てくるかもしれない。

今回はひとまず、encoder/decoder + EmbeddedChannelで変換する手法を使う。

## JettyでのPOSTリクエストボディのサイズ制限

実際にWebExtensionsからスクショの `data scheme url :base64encoded` を Spring(with Jetty) のform POSTエンドポイントにfetch APIでPOSTさせてみたら、Spring(というかJetty)で以下のエラーが発生。
```
2017-12-19 14:35:45,858 [qtp64605819-42]  WARN org.eclipse.jetty.server.HttpChannel - /attach-screenshot-upload
org.springframework.web.util.NestedServletException: Request processing failed; nested exception is org.eclipse.jetty.http.BadMessageException: 400: Unable to parse form content
(...)
Caused by: org.eclipse.jetty.http.BadMessageException: 400: Unable to parse form content
(...)
Caused by: java.lang.IllegalStateException: Form too large: 202666 > 200000
	at org.eclipse.jetty.server.Request.extractFormParameters(Request.java:516)
	at org.eclipse.jetty.server.Request.extractContentParameters(Request.java:454)
	at org.eclipse.jetty.server.Request.getParameters(Request.java:369)
	... 48 common frames omitted
```

参考:
- Jetty configuration : java.lang.IllegalStateException: Form too large: 239268 > 200000
  - https://github.com/ninjaframework/ninja/issues/484
- java - Form too Large Exception - Stack Overflow
  - https://stackoverflow.com/questions/3861455/form-too-large-exception
- JenkinsでリモートJOB実行する際に500エラー(java.lang.IllegalStateException: Form too large)とか出るときの対処 - Qiita
  - https://qiita.com/gotyoooo/items/821b9cf0e507c6d41220

公式:
- Setting Max Form Size
  - http://www.eclipse.org/jetty/documentation/current/setting-form-size.html
- http://download.eclipse.org/jetty/stable-9/apidocs/org/eclipse/jetty/server/handler/ContextHandler.html#setMaxFormContentSize-int-

もともと DoS 対策の観点もあるformサイズの最大値制限で、とりあえず今回は、あまりDoS対策の観点は考えずに、現実的にまず超えることはないだろう、100MB `(100 * 1024 * 1024)` を Jetty の contextHandler 設定箇所でハードコードして無事動作した。

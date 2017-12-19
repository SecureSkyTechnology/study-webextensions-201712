'use strict'

import Preference from './lib/Preference'
import thenChrome from 'then-chrome'

const BADGE_BGCOLOR_UPLOAD_SUCCESS = '#0000FF'
const BADGE_BGCOLOR_UPLOAD_FAILURE = '#FF0000'

let uploadSuccessCount = 0

async function getCurrentTab () {
  const tabs = await thenChrome.tabs.query({ active: true, currentWindow: true })
  if (!Array.isArray(tabs)) {
    return undefined
  }
  if (tabs.length !== 1) {
    return undefined
  }
  const tab = tabs[0]
  if (!tab.hasOwnProperty('id')) {
    return undefined
  }
  return tab
}

async function uploadImage (imageSrc) {
  const tab = await getCurrentTab()
  const pref = await Preference.get()
  // サーバ側にあわせた form POST を行う。実装の詳細はサーバ側のソースを参照のこと。
  // (2017-12-19時点では時間が無かったため、JSON APIのような気の利いたサーバ側実装まで手が回らなかった)
  const postbody = 'url=' + encodeURIComponent(tab.url) + '&dataSchemedImage=' + encodeURIComponent(imageSrc)
  const postheaders = new Headers()
  postheaders.append('Content-Type', 'application/x-www-form-urlencoded')
  postheaders.append('Content-Length', postbody.length.toString())
  fetch(pref.uploadUrl, {
    method: 'POST',
    mode: 'cors',
    headers: postheaders,

    /* 2017-12-19 時点では、サーバ側は通常HTMLフォームアプリとして画像添付に成功すると
     * 添付したログ表示用のURLにリダイレクトを行う。
     * つまり、JSON API のようには扱えない。(時間がなかったのでギブ)
     * => よって、成功か失敗かをどう判定するかだが、
     * 「302リダイレクトが発生したら成功、していなければ失敗」
     * として判定する。
     * そのため、redirectオプションにリダイレクト追従を行う "follow" を明示的に設定する。
     * これにより、response.ok && response.redirected == true なら成功と判断できる。
     * NOTE : Burp Proxy などを挟んでいた場合、もしもネットワーク的に到達できない状態
     * であったら、Burp Proxy 側で(Burpの設定にもよるが) 200 + Burp のエラー画面を返す。
     * このため、単に response.ok だけを見てしまっては、「最終的なエンドポイントがレスポンスを返した」のか、
     * 「エンドポイントまでのネットワーク問題でBurpがエラー画面を返した」のか、判別できない。
     * このため、上述のサーバ側のギブアップ結果と総合的に考えて、今の時点で
     * 「とりあえず動く」レベルを考えれば、response.ok && response.redirected == true
     * の判定がひとまず落とし所と考えた。
     */
    redirect: 'follow',

    body: postbody
  })
  .then((response) => {
    if (response.ok && response.redirected) {
      console.log('upload success (response.ok and response.redirected = true)')
      uploadSuccessCount++
      chrome.browserAction.setBadgeText({ text: String(uploadSuccessCount) })
      chrome.browserAction.setBadgeBackgroundColor({ color: BADGE_BGCOLOR_UPLOAD_SUCCESS })
    } else {
      console.log(`upload failure : response.ok=${response.ok}, redirected=${response.redirected}, status=${response.status}`, response)
      chrome.browserAction.setBadgeBackgroundColor({ color: BADGE_BGCOLOR_UPLOAD_FAILURE })
    }
  })
  .catch((error) => {
    console.log('fetch() error', error)
    chrome.browserAction.setBadgeBackgroundColor({ color: BADGE_BGCOLOR_UPLOAD_FAILURE })
  })
}

chrome.browserAction.onClicked.addListener(() => {
  chrome.tabs.captureVisibleTab((screenshotUrl) => {
    uploadImage(screenshotUrl)
  })
})

chrome.runtime.onInstalled.addListener((details) => {
  if (details.reason === 'install') {
    Preference.init()
  }
})

chrome.browserAction.setBadgeText({ text: String(uploadSuccessCount) })
chrome.browserAction.setBadgeBackgroundColor({ color: BADGE_BGCOLOR_UPLOAD_SUCCESS })

'use strict'

import thenChrome from 'then-chrome'

const BADGE_BG_COLOR_DEFAULT = '#FF0000'
const X_CUSTOM_HEADER_MATCH = 'https://www.securesky-tech.com/*'
const X_CUSTOM_HEADER_NAME = 'X-We-Exercise-2'

export default class Preference {
  static init () {
    chrome.storage.local.set({
      badgeBGColor: BADGE_BG_COLOR_DEFAULT,
      customHeaderMatch: X_CUSTOM_HEADER_MATCH,
      customHeaderName: X_CUSTOM_HEADER_NAME,
      insertHTML: ''
    })
  }

  static async get () {
    return thenChrome.storage.local.get()
  }

  static getBadgeBGColor (pref) {
        // これだけ install後のevent-listenerによるinit()が終わっていない状態で呼ばれる可能性が高いため、
        // 特別に pref 全体を引数に取り、キーがまだ無ければ(=install後のinit()呼び出しが完了していない)、
        // デフォルト値を返すようにしている、かなり例外的な処理。
        // もうちょっときれいな方法がありそうではあるが・・・。
    if (pref.hasOwnProperty('badgeBGColor')) {
      return pref.badgeBGColor
    } else {
      return BADGE_BG_COLOR_DEFAULT
    }
  }

  static setBadgeBGColor (color) {
    chrome.storage.local.set({
      badgeBGColor: color
    })
  }

    // static だったり、get/set なのにES6のgetter/setter使ってなかったりとイケてないところ多数ありますが、
    // 練習・習作ということでご容赦ください。

  static saveSettingPagePref (chm0, chn0, ih0) {
    chrome.storage.local.set({
      customHeaderMatch: chm0,
      customHeaderName: chn0,
      insertHTML: ih0
    })
  }

  static getCustomHeaderMatch (pref) {
    if (pref.hasOwnProperty('customHeaderMatch')) {
      return pref.customHeaderMatch
    } else {
      return X_CUSTOM_HEADER_MATCH
    }
  }

  static getCustomHeaderName (pref) {
    if (pref.hasOwnProperty('customHeaderName')) {
      return pref.customHeaderName
    } else {
      return X_CUSTOM_HEADER_NAME
    }
  }
}

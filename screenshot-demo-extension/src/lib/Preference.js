'use strict'

import thenChrome from 'then-chrome'

const UPLOAD_URL = 'http://127.0.0.1:10089/attach-screenshot-upload'

export default class Preference {
  static init () {
    chrome.storage.local.set({
      uploadUrl: UPLOAD_URL
    })
  }

  static async get () {
    return thenChrome.storage.local.get()
  }

    // static だったり、get/set なのにES6のgetter/setter使ってなかったりとイケてないところ多数ありますが、
    // 練習・習作ということでご容赦ください。

  static saveSettingPagePref (newurl) {
    chrome.storage.local.set({
      uploadUrl: newurl
    })
  }

  static getUploadUrl (pref) {
    if (pref.hasOwnProperty('uploadUrl')) {
      return pref.uploadUrl
    } else {
      return UPLOAD_URL
    }
  }
}

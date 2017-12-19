'use strict'

import Preference from './lib/Preference'

let globalCounter = 0
let tabbedCounter = {}

chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (!message.hasOwnProperty('command')) {
    sendResponse({error: 'unknown command'})
    return
  }
  let newTabbedCounter = 0
  switch (message.command) {
    case 'increment':
      globalCounter++
      if (message.hasOwnProperty('tabId')) {
        if (!tabbedCounter[message.tabId]) {
          tabbedCounter[message.tabId] = 0
        }
        tabbedCounter[message.tabId]++
        newTabbedCounter = tabbedCounter[message.tabId]
      }
      chrome.browserAction.setBadgeText({text: String(globalCounter)})
      sendResponse({ globalCount: globalCounter, tabbedCount: newTabbedCounter })
      break
    case 'get':
      if (message.hasOwnProperty('tabId') && tabbedCounter[message.tabId]) {
        newTabbedCounter = tabbedCounter[message.tabId]
      }
      sendResponse({ globalCount: globalCounter, tabbedCount: newTabbedCounter })
      break
    case 'set':
      if (message.hasOwnProperty('globalCount')) {
        globalCounter = message.globalCount
        chrome.browserAction.setBadgeText({text: String(globalCounter)})
      }
      break
    case 'badge-bg-color':
      if (message.hasOwnProperty('badgeBGColor')) {
        chrome.browserAction.setBadgeBackgroundColor({color: message.badgeBGColor})
        Preference.setBadgeBGColor(message.badgeBGColor)
      }
      break
    case 'update-custom-header-appender':
      setupCustomHeaderAppender(message.customHeaderMatch, message.customHeaderName)
      break
  }
})

chrome.runtime.onInstalled.addListener((details) => {
  if (details.reason === 'install') {
    Preference.init()
  }
})

let addCustomHeaderHandler

function setupCustomHeaderAppender (chm, chn) {
  if (addCustomHeaderHandler !== undefined) {
    chrome.webRequest.onBeforeSendHeaders.removeListener(addCustomHeaderHandler)
  }
  addCustomHeaderHandler = (e) => {
    e.requestHeaders.push({name: chn, value: String(globalCounter)})
    return {requestHeaders: e.requestHeaders}
  }
  chrome.webRequest.onBeforeSendHeaders.addListener(
        addCustomHeaderHandler,
        {urls: [chm]},
        ['blocking', 'requestHeaders']
    )
}

(async () => {
  const pref = await Preference.get()
  chrome.browserAction.setBadgeText({text: String(globalCounter)})
  chrome.browserAction.setBadgeBackgroundColor({color: Preference.getBadgeBGColor(pref)})

  const customHeaderMatch = Preference.getCustomHeaderMatch(pref)
  const customHeaderName = Preference.getCustomHeaderName(pref)
  setupCustomHeaderAppender(customHeaderMatch, customHeaderName)
})()

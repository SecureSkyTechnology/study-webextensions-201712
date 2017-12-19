'use strict'

import Preference from './lib/Preference'

const customHeaderMatchField = document.querySelector("input[name='customHeaderMatch']")
const customHeaderNameField = document.querySelector("input[name='customHeaderName']")
const insertHTMLField = document.querySelector("input[name='insertHTML']")

async function load () {
  const pref = await Preference.get()
  customHeaderMatchField.value = pref.customHeaderMatch
  customHeaderNameField.value = pref.customHeaderName
  insertHTMLField.value = pref.insertHTML
}

load()
document.querySelector('form').addEventListener('submit', () => {
  Preference.saveSettingPagePref(
        customHeaderMatchField.value,
        customHeaderNameField.value,
        insertHTMLField.value)
  chrome.runtime.sendMessage({
    command: 'update-custom-header-appender',
    customHeaderMatch: customHeaderMatchField.value,
    customHeaderName: customHeaderNameField.value
  })
})
document.querySelector('form').addEventListener('reset', () => {
  load()
})
console.log('initialized settings.js')

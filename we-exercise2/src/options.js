'use strict'

import thenChrome from 'then-chrome'
import Preference from './lib/Preference'

const badgeBGColorField = document.querySelector('#badgeBGColor')
const badgeCountResetToField = document.querySelector('#badgeCountResetTo')

async function load () {
  const pref = await Preference.get()
  badgeBGColorField.value = pref.badgeBGColor
  const response = await thenChrome.runtime.sendMessage({command: 'get'})
  badgeCountResetToField.value = response.globalCount
}

load()
document.querySelector('form').addEventListener('submit', () => {
  const newcnt = parseInt(badgeCountResetToField.value, 10)
  chrome.runtime.sendMessage({command: 'set', globalCount: newcnt})
  chrome.runtime.sendMessage({command: 'badge-bg-color', badgeBGColor: badgeBGColorField.value})
})
document.querySelector('form').addEventListener('reset', () => {
  load()
})
console.log('initialized options.js')

'use strict'

import Preference from './lib/Preference'

const uploadUrlField = document.querySelector("input[name='uploadUrl']")

async function load () {
  const pref = await Preference.get()
  uploadUrlField.value = pref.uploadUrl
}

load()
document.querySelector('form').addEventListener('submit', () => {
  Preference.saveSettingPagePref(uploadUrlField.value)
})
document.querySelector('form').addEventListener('reset', () => {
  load()
})
console.log('initialized options.js')

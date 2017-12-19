'use strict'

import Person from './lib/Person'
import thenChrome from 'then-chrome'
import bowser from 'bowser'

const bob = new Person('Bob', 15, 'cooking')
console.log(bob.hello());
[...document.querySelectorAll('#greeting p')].forEach(greetp => {
  const greeting = greetp.textContent
  greetp.addEventListener('click', () => {
    window.alert(`you clicked ${greeting}`)
    console.log(`clicked greeting = [${greeting}]`)
  })
})

async function getCurrentTab () {
  const tabs = await thenChrome.tabs.query({active: true, currentWindow: true})
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

const counterDom = document.querySelector('#counter')

function updateCount (response) {
  const globalCount = response.globalCount
  const tabbedCount = response.tabbedCount
  counterDom.textContent = `global:${globalCount} / tab:${tabbedCount}`
}

async function incrementCount () {
  const tab = await getCurrentTab()
  const response = await thenChrome.runtime.sendMessage({command: 'increment', tabId: tab.id})
  updateCount(response)
}

async function refreshCount () {
  const tab = await getCurrentTab()
  const response = await thenChrome.runtime.sendMessage({command: 'get', tabId: tab.id})
  updateCount(response)
}

async function init () {
  const coutupDom = document.querySelector('#countup')
  coutupDom.addEventListener('click', () => {
    incrementCount()
  })
  refreshCount()
  const tab = await getCurrentTab()
  const pageTitleDom = document.querySelector('#page-title')
  console.log(`url:[${tab.url}], id:[${tab.id}], title:[${tab.title}]`)
  pageTitleDom.textContent = 'title[' + tab.title + ']'
  document.querySelector('#open-option').addEventListener('click', () => {
    if (bowser.msedge) {
      window.alert('MS-Edge is not supported yet.')
    } else {
      chrome.runtime.openOptionsPage()
    }
  })
  document.querySelector('#open-setting').addEventListener('click', () => {
    chrome.tabs.create({url: chrome.extension.getURL('settings.html')})
  })
  console.log('initialized')
}

init()

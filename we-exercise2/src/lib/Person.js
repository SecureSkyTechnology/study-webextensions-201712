'use strict'

export default class Person {
  constructor (name, age, hobby) {
    this.name = name
    this.age = age
    this.hobby = hobby
  }

  hello () {
    return 'hello, my name is ' + this.name + ', age is ' + this.age + ', hobby is ' + this.hobby + '.'
  }

  get name () {
    return this._name
  }

  set name (v) {
    this._name = v
  }

  get age () {
    return this._age
  }

  set age (v) {
    this._age = v
  }

  get hobby () {
    return this._hobby
  }

  set hobby (v) {
    this._hobby = v
  }
}

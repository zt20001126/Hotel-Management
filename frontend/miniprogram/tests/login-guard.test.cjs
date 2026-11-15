const { test } = require('node:test')
const assert = require('node:assert/strict')
const vm = require('node:vm')
const fs = require('node:fs')
const path = require('node:path')

function setup(token = '', userInfo = null) {
  const storage = { token, userInfo }
  const calls = { navigation: [], requests: [] }
  let app
  const wx = {
    getStorageSync: key => storage[key],
    setStorageSync: (key, value) => { storage[key] = value },
    removeStorageSync: key => { delete storage[key] },
    reLaunch: options => calls.navigation.push(options.url),
    request: options => calls.requests.push(options)
  }
  vm.runInNewContext(fs.readFileSync(path.join(__dirname, '../app.js'), 'utf8'), {
    App: value => { app = value }, wx, getCurrentPages: () => [{ route: 'pages/index/index' }], console
  })
  app.onLaunch()
  return { app, calls, storage }
}

test('未登录进入小程序时跳转登录页', () => {
  const { app, calls } = setup()
  app.onShow({ path: 'pages/index/index' })
  assert.deepEqual(calls.navigation, ['/pages/login/login'])
})

test('已登录宾客进入时校验令牌，成功后保持登录', () => {
  const { app, calls } = setup('guest-token', {})
  app.onShow({ path: 'pages/index/index' })
  assert.ok(calls.requests[0].url.endsWith('/guest/current'))
  calls.requests[0].success({ statusCode: 200, data: { code: 200 } })
  assert.equal(app.globalData.token, 'guest-token')
  assert.equal(calls.navigation.length, 0)
})

test('员工令牌失效时清除登录状态并跳转登录页', () => {
  const { app, calls, storage } = setup('employee-token', { role: 'owner' })
  app.onShow({ path: 'pages/index/index' })
  assert.ok(calls.requests[0].url.endsWith('/employee/current'))
  calls.requests[0].success({ statusCode: 200, data: { code: 401 } })
  assert.equal(storage.token, undefined)
  assert.deepEqual(calls.navigation, ['/pages/login/login'])
})

test('旧请求的失效响应不会清除新登录状态', () => {
  const { app, calls } = setup('old-token', {})
  app.onShow({ path: 'pages/index/index' })
  app.login('new-token', {})
  calls.requests[0].success({ statusCode: 200, data: { code: 401 } })
  assert.equal(app.globalData.token, 'new-token')
  assert.equal(calls.navigation.length, 0)
})

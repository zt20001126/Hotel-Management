/* 在本机临时浏览器和模拟接口中验证第三方日历，不访问业务服务。 */
const http = require('node:http')
const fs = require('node:fs')
const os = require('node:os')
const path = require('node:path')
const { spawn } = require('node:child_process')
const assert = require('node:assert/strict')

const sleep = ms => new Promise(resolve => setTimeout(resolve, ms))
const profile = fs.mkdtempSync(path.join(os.tmpdir(), 'external-calendar-qa-'))
let chrome, vite, server, socket
let failCalendar = false

async function waitUntil(test, description) {
  for (let attempt = 0; attempt < 150; attempt++) {
    try { const value = await test(); if (value) return value } catch (_) { /* 服务启动中 */ }
    await sleep(100)
  }
  throw new Error(`等待超时: ${description}`)
}

async function run() {
  server = http.createServer((req, res) => {
    const url = new URL(req.url, 'http://localhost')
    let data
    if (url.pathname === '/api/external/calendar') {
      if (failCalendar) { res.writeHead(500); res.end('unavailable'); return }
      const start = url.searchParams.get('startDate')
      const end = new Date(`${url.searchParams.get('endDate')}T00:00:00Z`)
      end.setUTCDate(end.getUTCDate() + 1)
      data = [{ id: 7, roomId: 77, roomName: '历史房间', platformName: '携程', status: 'BOOKED', checkInDate: start, checkOutDate: end.toISOString().slice(0, 10) }]
    } else if (url.pathname === '/api/external/statistics') {
      const bucket = { name: '携程', orderCount: 1, nights: 2, totalAmount: 500, platformFee: 20, anchorFee: 10, netProfit: 470 }
      data = { total: bucket, daily: [{ ...bucket, name: '2026-11-01' }], platforms: [bucket], anchors: [{ ...bucket, name: '主播甲' }], rooms: [{ ...bucket, name: '101' }] }
    } else if (url.pathname === '/api/external/orders/7') {
      data = { id: 7, roomId: 77, roomName: '历史房间', platformId: 1, platformName: '携程', anchorId: null, status: 'CHECKED_OUT', checkInDate: '2026-11-01', checkOutDate: '2026-11-02', totalAmount: 100, platformFee: 5, anchorFee: 0, guestCount: 1 }
    } else if (url.pathname === '/api/external/orders') data = { records: [], total: 0 }
    else if (url.pathname === '/api/room/list') data = [{ id: 1, roomNumber: '101' }]
    else if (url.pathname === '/api/external/platforms') data = [{ id: 1, name: '携程', status: 1 }]
    else data = []
    res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' })
    res.end(JSON.stringify({ code: 200, data }))
  })
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve))
  const backendPort = server.address().port
  const portServer = http.createServer()
  await new Promise(resolve => portServer.listen(0, '127.0.0.1', resolve))
  const frontendPort = portServer.address().port
  await new Promise(resolve => portServer.close(resolve))
  vite = spawn(process.execPath, [path.resolve('node_modules/vite/bin/vite.js'), '--host', '127.0.0.1', '--port', String(frontendPort), '--strictPort'], {
    cwd: process.cwd(), env: { ...process.env, VITE_API_PROXY_TARGET: `http://127.0.0.1:${backendPort}` }, windowsHide: true, stdio: 'ignore'
  })
  await waitUntil(async () => (await fetch(`http://127.0.0.1:${frontendPort}/`)).ok, 'Vite 启动')
  const executable = process.env.CHROME_PATH || 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
  chrome = spawn(executable, ['--headless=new', '--disable-gpu', '--no-first-run', '--remote-debugging-port=0', `--user-data-dir=${profile}`, 'about:blank'], { windowsHide: true, stdio: 'ignore' })
  const activePort = await waitUntil(() => {
    const file = path.join(profile, 'DevToolsActivePort')
    return fs.existsSync(file) && fs.readFileSync(file, 'utf8').split('\n')[0]
  }, 'Chrome 启动')
  const target = await (await fetch(`http://127.0.0.1:${activePort}/json/new?about:blank`, { method: 'PUT' })).json()
  socket = new WebSocket(target.webSocketDebuggerUrl)
  await new Promise((resolve, reject) => { socket.onopen = resolve; socket.onerror = reject })
  let id = 0
  const pending = new Map()
  socket.onmessage = event => { const result = JSON.parse(event.data); if (pending.has(result.id)) { const promise = pending.get(result.id); pending.delete(result.id); result.error ? promise.reject(new Error(result.error.message)) : promise.resolve(result.result) } }
  function send(method, params = {}) { return new Promise((resolve, reject) => { const current = ++id; pending.set(current, { resolve, reject }); socket.send(JSON.stringify({ id: current, method, params })) }) }
  async function evaluate(expression) { const result = await send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true }); if (result.exceptionDetails) throw new Error(result.exceptionDetails.text); return result.result.value }
  await send('Page.enable')
  await send('Page.addScriptToEvaluateOnNewDocument', { source: `try { localStorage.setItem('token','qa-token'); localStorage.setItem('userInfo', JSON.stringify({role:'owner',username:'qa'})); } catch (_) {}` })
  await send('Page.navigate', { url: `http://127.0.0.1:${frontendPort}/external/calendar` })
  await waitUntil(() => evaluate(`document.body.innerText.includes('历史房间') && document.body.innerText.includes('携程')`), '历史房间和平台订单显示')
  assert.equal(await evaluate(`document.querySelectorAll('thead th').length`), new Date().getDate() > 0 ? new Date(new Date().getFullYear(), new Date().getMonth() + 1, 0).getDate() + 1 : 0)
  await evaluate(`Array.from(document.querySelectorAll('tbody tr')).find(row => row.innerText.includes('历史房间')).querySelector('td').click()`)
  await waitUntil(() => evaluate(`location.pathname === '/external/orders' && document.body.innerText.includes('订单详情')`), '点击历史订单打开详情')
  await waitUntil(() => evaluate(`document.querySelector('.el-dialog')?.innerText.includes('历史房间')`), '详情显示已删除房间名称')
  await send('Page.navigate', { url: `http://127.0.0.1:${frontendPort}/external/calendar` })
  await waitUntil(() => evaluate(`document.body.innerText.includes('历史房间')`), '返回日历')
  await evaluate(`Array.from(document.querySelectorAll('.el-radio-button')).find(e => e.innerText.trim() === '周').click()`)
  await waitUntil(async () => (await evaluate(`document.querySelectorAll('thead th').length`)) === 8, '周视图七天')
  await evaluate(`Array.from(document.querySelectorAll('.el-radio-button')).find(e => e.innerText.trim() === '日').click()`)
  await waitUntil(async () => (await evaluate(`document.querySelectorAll('thead th').length`)) === 2, '日视图一天')
  failCalendar = true
  await evaluate(`Array.from(document.querySelectorAll('button')).find(e => e.innerText.includes('下一日')).click()`)
  await waitUntil(() => evaluate(`document.body.innerText.includes('房态或房间列表加载失败')`), '接口失败提示')
  assert.equal(await evaluate(`document.querySelectorAll('.grid table').length`), 0, '接口失败时不能继续显示旧房态')
  await send('Page.navigate', { url: `http://127.0.0.1:${frontendPort}/external/reports` })
  await waitUntil(() => evaluate(`document.body.innerText.includes('470') && document.body.innerText.includes('2026-11-01')`), '经营统计总览和每日明细')
  await evaluate(`Array.from(document.querySelectorAll('[role="tab"]')).find(e => e.innerText.includes('主播统计')).click()`)
  await waitUntil(() => evaluate(`document.body.innerText.includes('主播甲')`), '主播维度切换')
  await send('Page.navigate', { url: `http://127.0.0.1:${frontendPort}/external/settings` })
  await waitUntil(() => evaluate(`document.body.innerText.includes('携程') && document.body.innerText.includes('新增平台')`), '平台设置')
  console.log('通过：历史房间及详情、日周月列数、失败提示、报表维度和平台设置')
}

run().catch(error => { console.error(error); process.exitCode = 1 }).finally(async () => {
  if (socket) socket.close()
  if (chrome) chrome.kill()
  if (vite) vite.kill()
  if (server) server.close()
  await sleep(300)
  const tempRoot = path.resolve(os.tmpdir()) + path.sep
  const resolvedProfile = path.resolve(profile)
  if (resolvedProfile.startsWith(tempRoot) && path.basename(resolvedProfile).startsWith('external-calendar-qa-')) {
    try { fs.rmSync(resolvedProfile, { recursive: true, force: true, maxRetries: 3, retryDelay: 100 }) } catch (_) { /* Chrome 尚在退出时由系统清理临时目录 */ }
  }
})

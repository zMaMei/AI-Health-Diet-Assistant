/**
 * Batch import: for each image in ../images/ → AI recognition → save diet records
 * Images mapped to 2026-06-04 ~ 2026-06-20, lunch+dinner per day, extras to earlier dates.
 * All records for user zMa (id=3).
 *
 * Usage: node batch-import.js
 * Requires: server running on localhost:8080
 */

const fs = require('fs')
const path = require('path')
const http = require('http')

const BASE = 'http://localhost:8080'
const API = '/api'
const IMG_DIR = path.resolve(__dirname, '..', 'images')
const USERNAME = 'zMa'
const PASSWORD = '123456'

// ── helpers ──────────────────────────────────────────────
function post(url, body, headers = {}) {
  return new Promise((resolve, reject) => {
    const u = new URL(url)
    const isJson = typeof body === 'string'
    const opts = {
      hostname: u.hostname, port: u.port, path: u.pathname + u.search,
      method: 'POST',
      headers: isJson
        ? { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(body), ...headers }
        : { ...body.getHeaders(), ...headers },
    }
    const req = http.request(opts, res => {
      let data = ''
      res.on('data', c => data += c)
      res.on('end', () => {
        try { resolve({ status: res.statusCode, body: JSON.parse(data) }) }
        catch { resolve({ status: res.statusCode, body: data }) }
      })
    })
    req.on('error', reject)
    if (isJson) req.write(body)
    else body.pipe(req)
  })
}

function sleep(ms) { return new Promise(r => setTimeout(r, ms)) }

// ── login ────────────────────────────────────────────────
async function login() {
  console.log('🔑 登录中...')
  const res = await post(`${BASE}${API}/auth/login`,
    JSON.stringify({ username: USERNAME, password: PASSWORD }))
  if (res.status !== 200 || !res.body?.data?.token) {
    throw new Error('登录失败: ' + JSON.stringify(res.body))
  }
  console.log('✅ 登录成功, token=' + res.body.data.token.substring(0, 20) + '...')
  return res.body.data.token
}

// ── recognize image ──────────────────────────────────────
async function recognizeImage(filePath, token) {
  const { default: FormData } = await import('form-data')
  const { default: fetch } = await import('node-fetch')
  // Actually let me use native http...
  // Hmm, multipart is complex with raw http. Let me use a simpler approach with fetch.
}

// Better: use the built-in approach with FormData polyfill

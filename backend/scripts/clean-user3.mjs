const BASE = 'http://localhost:8080'
const TOKEN = (await fetch(`${BASE}/api/auth/login`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username: 'zMa', password: '123456' }),
}).then(r => r.json())).data.token
console.log('✅ 已登录')

const auth = { 'Authorization': `Bearer ${TOKEN}` }

// Delete all diet records for date range
for (let d = 3; d <= 21; d++) {
  const date = `2026-06-${String(d).padStart(2, '0')}`
  const r = await fetch(`${BASE}/api/diet-records?date=${date}`, { headers: auth })
  const data = await r.json()
  const records = data.data || []
  if (records.length) {
    for (const rec of records) {
      await fetch(`${BASE}/api/diet-records/${rec.id}`, { method: 'DELETE', headers: auth })
    }
    console.log(`  🗑️ ${date}: ${records.length} 条`)
  }
}

// Delete meal photos
for (let d = 3; d <= 21; d++) {
  const date = `2026-06-${String(d).padStart(2, '0')}`
  const r = await fetch(`${BASE}/api/meal-photos?date=${date}`, { headers: auth })
  const data = await r.json()
  const photos = data.data || []
  for (const p of photos) {
    await fetch(`${BASE}/api/meal-photos/${p.id}`, { method: 'DELETE', headers: auth })
  }
  if (photos.length) console.log(`  📷 ${date}: ${photos.length} 张照片`)
}

// Delete voice records
for (let d = 3; d <= 21; d++) {
  const date = `2026-06-${String(d).padStart(2, '0')}`
  const r = await fetch(`${BASE}/api/voice-records?date=${date}`, { headers: auth })
  const data = await r.json()
  const voices = data.data || []
  for (const v of voices) {
    await fetch(`${BASE}/api/voice-records/${v.id}`, { method: 'DELETE', headers: auth })
  }
  if (voices.length) console.log(`  🎤 ${date}: ${voices.length} 条语音`)
}

console.log('✅ 清理完成')

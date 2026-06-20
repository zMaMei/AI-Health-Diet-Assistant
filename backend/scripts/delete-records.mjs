import { resolve } from 'path'

const BASE = 'http://localhost:8080'
const TOKEN = (await fetch(`${BASE}/api/auth/login`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username: 'zMa', password: '123456' }),
}).then(r => r.json())).data.token
console.log('✅ 已登录')

async function del(date, mealType) {
  const res = await fetch(`${BASE}/api/diet-records?date=${date}`, {
    headers: { 'Authorization': `Bearer ${TOKEN}` },
  })
  const data = await res.json()
  let records = (data.data || []).filter(r => !mealType || r.mealType === mealType)
  console.log(`${date} ${mealType || '全部'}: ${records.length} 条`)
  for (const r of records) {
    await fetch(`${BASE}/api/diet-records/${r.id}`, {
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${TOKEN}` },
    })
    console.log(`  🗑️ ${r.mealType} ${r.foodName}`)
  }
}

await del('2026-06-19', '晚餐')
await del('2026-06-20', null)
console.log('✅ 删除完成')

const BASE = 'http://localhost:8080'
const TOKEN = (await fetch(`${BASE}/api/auth/login`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username: 'zMa', password: '123456' }),
}).then(r => r.json())).data.token
console.log('✅ 已登录\n')

const auth = { 'Authorization': `Bearer ${TOKEN}` }

// Delete specific meals
const meals = [
  { date: '2026-06-19', meal: '晚餐', file: 'OKND3015.JPG' },
  { date: '2026-06-20', meal: '午餐', file: 'SOZA3348.JPG' },
  { date: '2026-06-20', meal: '晚餐', file: 'TWZO1322.JPG' },
]

for (const m of meals) {
  // Delete diet records
  const r = await fetch(`${BASE}/api/diet-records?date=${m.date}`, { headers: auth })
  const data = await r.json()
  const records = (data.data || []).filter(r => r.mealType === m.meal)
  console.log(`🗑️ ${m.date} ${m.meal}: ${records.length} 条`)
  for (const rec of records)
    await fetch(`${BASE}/api/diet-records/${rec.id}`, { method: 'DELETE', headers: auth })

  // Delete meal photos
  const pr = await fetch(`${BASE}/api/meal-photos?date=${m.date}&mealType=${m.meal}`, { headers: auth })
  const pdata = await pr.json()
  for (const p of (pdata.data || []))
    await fetch(`${BASE}/api/meal-photos/${p.id}`, { method: 'DELETE', headers: auth })
}

console.log('\n📸 重新导入...\n')
import { readFileSync } from 'fs'
import { resolve } from 'path'
const IMG_DIR = resolve(import.meta.dirname, '..', 'images')

for (const m of meals) {
  const fp = resolve(IMG_DIR, m.file)
  const blob = new Blob([readFileSync(fp)], { type: 'image/jpeg' })
  const form = new FormData()
  form.append('image', blob, m.file)
  const res = await fetch(`${BASE}/api/food/recognize`, { method: 'POST', headers: auth, body: form })
  const j = await res.json()
  const candidates = j.data?.candidates || []
  const imageUrl = j.data?.imageUrl || null
  console.log(`${m.file} → ${m.date} ${m.meal}: ${candidates.length} 种食物`)

  for (const c of candidates) {
    const body = JSON.stringify({
      foodName: c.foodName, mealType: m.meal, amount: c.defaultAmount || 1, source: 'photo',
      recordTime: `${m.date}T12:00:00`,
      calorie: c.nutritionPreview?.calorie, protein: c.nutritionPreview?.protein,
      fat: c.nutritionPreview?.fat, carbohydrate: c.nutritionPreview?.carbohydrate,
      sugar: c.nutritionPreview?.sugar, sodium: c.nutritionPreview?.sodium,
    })
    await fetch(`${BASE}/api/diet-records`, { method: 'POST', headers: { ...auth, 'Content-Type': 'application/json' }, body })
    console.log(`  ✅ ${c.foodName} ${c.nutritionPreview?.calorie || 0}kcal`)
  }
  if (imageUrl) {
    await fetch(`${BASE}/api/meal-photos`, {
      method: 'POST', headers: { ...auth, 'Content-Type': 'application/json' },
      body: JSON.stringify({ recordDate: m.date, mealType: m.meal, imageUrl }),
    })
  }
}

console.log('\n✅ 完成')

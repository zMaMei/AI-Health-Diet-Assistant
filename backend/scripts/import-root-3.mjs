import { readdirSync, readFileSync } from 'fs'
import { resolve, basename } from 'path'

const BASE = 'http://localhost:8080'
const IMG_DIR = resolve(import.meta.dirname, '..', 'images')

const t = await (await fetch(`${BASE}/api/auth/login`, {
  method: 'POST', headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username: 'root', password: '123456' }),
})).json()
const TOKEN = t.data.token
const auth = { 'Authorization': `Bearer ${TOKEN}` }
console.log(`✅ 已登录 root (id=${t.data.userId})\n`)

// Delete 3 meals
const meals = [
  { date: '2026-06-19', meal: '晚餐' },
  { date: '2026-06-20', meal: '午餐' },
  { date: '2026-06-20', meal: '晚餐' },
]
for (const m of meals) {
  const r = await fetch(`${BASE}/api/diet-records?date=${m.date}`, { headers: auth })
  const records = ((await r.json()).data || []).filter(r => r.mealType === m.meal)
  for (const rec of records)
    await fetch(`${BASE}/api/diet-records/${rec.id}`, { method: 'DELETE', headers: auth })
  const pr = await fetch(`${BASE}/api/meal-photos?date=${m.date}&mealType=${m.meal}`, { headers: auth })
  for (const p of ((await pr.json()).data || []))
    await fetch(`${BASE}/api/meal-photos/${p.id}`, { method: 'DELETE', headers: auth })
  console.log(`🗑️ ${m.date} ${m.meal}: ${records.length}条`)
}

// Import 3 new images
const files = readdirSync(IMG_DIR).filter(f => /\.(jpg|jpeg|png|gif|webp)$/i.test(f)).sort()
console.log(`\n📸 ${files.length} 张新图片\n`)

for (let i = 0; i < files.length; i++) {
  const m = meals[i]
  const fp = resolve(IMG_DIR, files[i])
  const ext = files[i].split('.').pop().toLowerCase()
  const mime = ext === 'png' ? 'image/png' : 'image/jpeg'
  const blob = new Blob([readFileSync(fp)], { type: mime })
  const form = new FormData()
  form.append('image', blob, files[i])

  const res = await fetch(`${BASE}/api/food/recognize`, { method: 'POST', headers: auth, body: form })
  const j = await res.json()
  const candidates = j.data?.candidates || []
  const imageUrl = j.data?.imageUrl || null
  console.log(`${files[i]} → ${m.date} ${m.meal}: ${candidates.length} 种`)

  for (const c of candidates) {
    await fetch(`${BASE}/api/diet-records`, {
      method: 'POST', headers: { ...auth, 'Content-Type': 'application/json' },
      body: JSON.stringify({
        foodName: c.foodName, mealType: m.meal, amount: c.defaultAmount || 1, source: 'photo',
        recordTime: `${m.date}T12:00:00`,
        calorie: c.nutritionPreview?.calorie, protein: c.nutritionPreview?.protein,
        fat: c.nutritionPreview?.fat, carbohydrate: c.nutritionPreview?.carbohydrate,
        sugar: c.nutritionPreview?.sugar, sodium: c.nutritionPreview?.sodium,
      }),
    })
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

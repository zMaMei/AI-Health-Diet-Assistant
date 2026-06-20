/**
 * Batch import: for each image → AI recognition → save diet records.
 * Images mapped 2026-06-04 ~ 2026-06-20, lunch+dinner per day.
 * Extra images go to earlier dates (2026-06-03, 2026-06-02, ...).
 * All for user zMa (id=3).
 *
 * Usage: node batch-import.mjs
 * Requires server on localhost:8080
 */
import { readdirSync, readFileSync } from 'fs'
import { resolve, basename } from 'path'

const BASE = 'http://localhost:8080'
const IMG_DIR = resolve(import.meta.dirname || '.', '..', 'images')
const USERNAME = 'root'
const PASSWORD = '123456'

// ── helpers ──────────────────────────────────────────────
function formatLocalDate(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function sleep(ms) { return new Promise(r => setTimeout(r, ms)) }

// ── login ────────────────────────────────────────────────
async function login() {
  console.log('🔑 登录中...')
  const res = await fetch(`${BASE}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: USERNAME, password: PASSWORD }),
  })
  const data = await res.json()
  if (data.code !== 200 || !data.data?.token) {
    throw new Error('登录失败: ' + JSON.stringify(data))
  }
  console.log(`✅ 登录成功, token=${data.data.token.substring(0, 20)}...`)
  return data.data.token
}

// ── recognize one image ──────────────────────────────────
async function recognizeImage(filePath, token) {
  const fileName = basename(filePath)
  const fileBytes = readFileSync(filePath)
  const blob = new Blob([fileBytes], { type: 'image/jpeg' })

  const form = new FormData()
  form.append('image', blob, fileName)

  const res = await fetch(`${BASE}/api/food/recognize`, {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${token}` },
    body: form,
  })
  const data = await res.json()
  if (data.code !== 200) {
    throw new Error(`识别失败: ${JSON.stringify(data)}`)
  }
  const candidates = data.data?.candidates || []
  const imageUrl = data.data?.imageUrl || null
  return { candidates, imageUrl }
}

// ── save one diet record ─────────────────────────────────
async function saveRecord(record, token) {
  const res = await fetch(`${BASE}/api/diet-records`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
    body: JSON.stringify(record),
  })
  const data = await res.json()
  return data
}

// ── save meal photo ──────────────────────────────────────
async function saveMealPhoto(photoData, token) {
  const res = await fetch(`${BASE}/api/meal-photos`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
    body: JSON.stringify(photoData),
  })
  return await res.json()
}

// ── main ─────────────────────────────────────────────────
async function main() {
  // 1. login
  const token = await login()

  // 2. get sorted image list
  const files = readdirSync(IMG_DIR)
    .filter(f => /\.(jpg|jpeg|png|gif|webp|heic|heif)$/i.test(f))
    .sort()
  console.log(`📁 找到 ${files.length} 张图片`)

  // 3. build date/meal mapping
  // 17 days (June 4-20) × 2 meals = 34 slots
  // Extra images → earlier dates starting from June 3
  const startDate = new Date(2026, 5, 4) // June 4, 2026 (month is 0-indexed)
  const assignments = []
  const baseDays = 17
  const mealsPerDay = 2  // lunch, dinner

  for (let i = 0; i < files.length; i++) {
    let date, mealType
    if (i < baseDays * mealsPerDay) {
      // normal range: June 4-20
      const dayOffset = Math.floor(i / mealsPerDay)
      const mealIdx = i % mealsPerDay
      const d = new Date(startDate)
      d.setDate(d.getDate() + dayOffset)
      date = formatLocalDate(d)
      mealType = mealIdx === 0 ? '午餐' : '晚餐'
    } else {
      // extra: go backwards from June 3
      const extraIdx = i - baseDays * mealsPerDay
      const d = new Date(2026, 5, 3) // June 3
      d.setDate(d.getDate() - extraIdx)
      date = formatLocalDate(d)
      mealType = '晚餐'  // extras are evening meals
    }
    assignments.push({ file: files[i], date, mealType })
  }

  console.log('📋 分配方案 (前5+后5):')
  for (const a of assignments.slice(0, 5)) {
    console.log(`  ${a.date} ${a.mealType} ← ${a.file}`)
  }
  console.log('  ...')
  for (const a of assignments.slice(-5)) {
    console.log(`  ${a.date} ${a.mealType} ← ${a.file}`)
  }

  // 4. process each image
  let totalSaved = 0
  let totalFailed = 0

  for (let i = 0; i < assignments.length; i++) {
    const { file, date, mealType } = assignments[i]
    const filePath = resolve(IMG_DIR, file)
    console.log(`\n[${i + 1}/${assignments.length}] ${file} → ${date} ${mealType}`)

    try {
      // 4a. AI recognition
      const { candidates, imageUrl } = await recognizeImage(filePath, token)
      console.log(`  🤖 识别到 ${candidates.length} 种食物`)

      if (candidates.length === 0) {
        console.log(`  ⚠️ 未识别到食物，跳过`)
        totalFailed++
        continue
      }

      // 4b. Save each candidate as diet record
      for (const c of candidates) {
        const record = {
          foodName: c.foodName,
          mealType,
          amount: c.defaultAmount || 1,
          source: 'photo',
          recordTime: `${date}T12:00:00`,
          calorie: c.nutritionPreview?.calorie,
          protein: c.nutritionPreview?.protein,
          fat: c.nutritionPreview?.fat,
          carbohydrate: c.nutritionPreview?.carbohydrate,
          sugar: c.nutritionPreview?.sugar,
          sodium: c.nutritionPreview?.sodium,
        }
        const saved = await saveRecord(record, token)
        if (saved.code === 200) {
          totalSaved++
          console.log(`  ✅ 保存: ${c.foodName} ${c.nutritionPreview?.calorie || 0}kcal`)
        } else {
          totalFailed++
          console.log(`  ❌ 保存失败: ${c.foodName} - ${saved.message || 'unknown'}`)
        }
      }

      // 4c. Save meal photo
      if (imageUrl) {
        await saveMealPhoto({ recordDate: date, mealType, imageUrl }, token)
      }

    } catch (e) {
      console.log(`  ❌ 错误: ${e.message}`)
      totalFailed++
    }

    // Small delay between images to avoid rate limiting
    await sleep(1000)
  }

  console.log(`\n\n🎉 完成! 成功保存 ${totalSaved} 条记录, 失败 ${totalFailed}`)
}

main().catch(e => { console.error('FATAL:', e.message); process.exit(1) })

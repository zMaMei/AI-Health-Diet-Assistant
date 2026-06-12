import { ref } from 'vue'

const visible = ref(false)
const message = ref('')
let timer = null

function show(msg, duration = 2500) {
  // 清除上一个定时器，避免消息堆积
  if (timer) clearTimeout(timer)
  message.value = msg
  visible.value = true
  timer = setTimeout(() => {
    visible.value = false
    timer = null
  }, duration)
}

export default { visible, message, show }

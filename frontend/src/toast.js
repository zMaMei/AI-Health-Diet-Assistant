/* 全局Toast消息提示 */
import { ref } from 'vue'

/* Toast可见状态 */
const visible = ref(false)
/* Toast消息内容 */
const message = ref('')
let timer = null

/* 显示Toast消息 */
function show(msg, duration = 2500) {
  /* 清除上一个定时器，避免消息堆积 */
  if (timer) clearTimeout(timer)
  message.value = msg
  visible.value = true
  timer = setTimeout(() => {
    visible.value = false
    timer = null
  }, duration)
}

export default { visible, message, show }

<!-- 第三方平台房态日历；不表示小程序或其他渠道的可售房态。 -->
<template>
  <el-alert title="仅显示第三方平台订单，不代表小程序房态或全渠道可售情况" type="warning" :closable="false" />
  <el-card class="calendar-card">
    <div class="toolbar">
      <el-radio-group v-model="mode"><el-radio-button label="day">日</el-radio-button><el-radio-button label="week">周</el-radio-button><el-radio-button label="month">月</el-radio-button></el-radio-group>
      <el-button @click="move(-1)">上一{{ unitLabel }}</el-button>
      <el-date-picker v-model="anchor" type="date" value-format="YYYY-MM-DD" :clearable="false" />
      <el-button @click="move(1)">下一{{ unitLabel }}</el-button><el-button @click="anchor = dayjs().format('YYYY-MM-DD')">今天</el-button>
      <el-select v-model="platformId" clearable placeholder="全部平台" style="width:160px"><el-option v-for="p in platforms" :key="p.id" :label="p.name" :value="p.id" /></el-select>
    </div>
    <el-alert v-if="loadError || roomsError" title="房态或房间列表加载失败，请刷新重试；不能据此判断房间是否空闲" type="error" :closable="false" />
    <div v-else class="grid" v-loading="loading"><table><thead><tr><th>房间</th><th v-for="date in days" :key="date">{{ date }}</th></tr></thead>
      <tbody><tr v-for="room in displayRooms" :key="room.id"><th>{{ room.roomNumber }}{{ room.historical ? '（已删除）' : '' }}</th><td v-for="date in days" :key="date" @click="open(room,date)">
        <template v-if="findOrder(room.id,date)"><el-tag>{{ labels[findOrder(room.id,date).status] }}</el-tag><div>{{ findOrder(room.id,date).platformName }}</div></template><span v-else>{{ room.historical ? '—' : platformId ? '无匹配订单' : '空房' }}</span>
      </td></tr></tbody></table></div>
  </el-card>
</template>
<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { getCalendar, getPlatforms } from '@/api/external'
import { getAllRooms } from '@/api/room'
const router = useRouter(), mode = ref('month'), anchor = ref(dayjs().format('YYYY-MM-DD')), platformId = ref(null)
const platforms = ref([]), rooms = ref([]), orders = ref([]), loading = ref(false), loadError = ref(false), roomsError = ref(false)
const labels = { BOOKED: '已预订', CHECKED_IN: '已入住', CHECKED_OUT: '已退房' }
const unitLabel = computed(() => ({ day: '日', week: '周', month: '月' })[mode.value])
const days = computed(() => {
  const date = dayjs(anchor.value); const start = mode.value === 'week' ? date.subtract((date.day() + 6) % 7, 'day') : mode.value === 'month' ? date.startOf('month') : date
  const count = mode.value === 'week' ? 7 : mode.value === 'month' ? date.daysInMonth() : 1
  return Array.from({ length: count }, (_, i) => start.add(i, 'day').format('YYYY-MM-DD'))
})
const displayRooms = computed(() => {
  const active = rooms.value.map(room => ({ ...room, historical: false }))
  const seen = new Set(active.map(room => room.id))
  for (const order of orders.value) {
    if (!seen.has(order.roomId)) {
      active.push({ id: order.roomId, roomNumber: order.roomName, historical: true })
      seen.add(order.roomId)
    }
  }
  return active
})
function move(n) { anchor.value = dayjs(anchor.value).add(n, mode.value).format('YYYY-MM-DD') }
function findOrder(roomId, date) { return orders.value.find(o => o.roomId === roomId && o.checkInDate <= date && o.checkOutDate > date) }
function open(room, date) { const order = findOrder(room.id, date); if (!order && room.historical) return; router.push({ path: '/external/orders', query: order ? { orderId: order.id } : { roomId: room.id, date } }) }
let generation = 0
async function load() {
  const current = ++generation; loading.value = true; loadError.value = false
  try { const res = await getCalendar({ startDate: days.value[0], endDate: days.value.at(-1), platformId: platformId.value || undefined }); if (current === generation) orders.value = res.data }
  catch (error) { if (current === generation) { orders.value = []; loadError.value = true } }
  finally { if (current === generation) loading.value = false }
}
watch([days, platformId], load, { immediate: true })
getPlatforms({ includeDisabled: true }).then(res => { platforms.value = res.data })
getAllRooms().then(res => { rooms.value = res.data }).catch(() => { roomsError.value = true })
</script>
<style scoped>
.calendar-card { margin-top: 16px } .toolbar { display:flex; gap:10px; flex-wrap:wrap; margin-bottom:16px } .grid { overflow:auto } table { border-collapse:collapse; width:100% } th,td { border:1px solid #ebeef5; min-width:115px; padding:12px; text-align:center } td { cursor:pointer } td:hover { background:#f0f7ff }
</style>

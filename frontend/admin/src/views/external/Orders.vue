<!-- 手动录入和维护独立的第三方平台订单。 -->
<template>
  <el-alert title="此处仅管理第三方平台订单，不检查小程序订单房态" type="warning" :closable="false" />
  <el-card style="margin-top:16px"><div class="toolbar">
    <el-input v-model="filters.keyword" placeholder="系统或平台订单号" clearable style="width:200px" />
    <el-select v-model="filters.platformId" clearable placeholder="全部平台" style="width:150px"><el-option v-for="p in platforms" :key="p.id" :label="p.name" :value="p.id" /></el-select>
    <el-select v-model="filters.anchorId" clearable placeholder="全部主播" style="width:150px"><el-option v-for="a in anchors" :key="a.id" :label="a.name" :value="a.id" /></el-select>
    <el-select v-model="filters.roomId" clearable placeholder="全部房间" style="width:150px"><el-option v-for="r in rooms" :key="r.id" :label="r.roomNumber" :value="r.id" /></el-select>
    <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" />
    <el-select v-model="filters.status" clearable placeholder="全部状态" style="width:150px"><el-option v-for="(label,key) in labels" :key="key" :label="label" :value="key" /></el-select>
    <el-button type="primary" @click="search">查询</el-button><el-button type="primary" @click="create">新增订单</el-button>
  </div>
  <el-table v-loading="loading" :data="rows"><el-table-column prop="roomName" label="房间" /><el-table-column prop="platformName" label="平台" /><el-table-column prop="anchorName" label="主播" /><el-table-column prop="checkInDate" label="入住" width="110" /><el-table-column prop="checkOutDate" label="退房" width="110" /><el-table-column prop="nights" label="晚数" /><el-table-column prop="guestCount" label="人数" /><el-table-column prop="totalAmount" label="付款" /><el-table-column prop="platformFee" label="平台费" /><el-table-column prop="anchorFee" label="主播费" /><el-table-column prop="netProfit" label="净利润" /><el-table-column label="状态"><template #default="{row}">{{ labels[row.status] }}</template></el-table-column><el-table-column label="操作" width="220"><template #default="{row}"><el-button link type="primary" @click="show(row)">{{ row.status === 'BOOKED' ? '编辑' : '详情' }}</el-button><el-button v-if="row.status === 'BOOKED'" link type="primary" @click="change(row, 'CHECKED_IN')">入住</el-button><el-button v-if="row.status === 'BOOKED'" link type="danger" @click="change(row, 'CANCELLED')">取消</el-button><el-button v-if="row.status === 'CHECKED_IN'" link type="primary" @click="change(row, 'CHECKED_OUT')">退房</el-button></template></el-table-column></el-table>
  <el-pagination layout="total, prev, pager, next" :total="total" :page-size="filters.size" v-model:current-page="filters.page" @current-change="load" />
  </el-card>
  <el-dialog v-model="visible" :title="form.id ? '订单详情' : '新增平台订单'" width="650px"><el-form label-width="110px" :disabled="!!form.id && form.status !== 'BOOKED'">
    <el-form-item label="平台" required><el-select v-model="form.platformId"><el-option v-for="p in platforms" :key="p.id" :label="p.name" :value="p.id" :disabled="p.status !== 1 && p.id !== form.platformId" /></el-select></el-form-item>
    <el-form-item label="平台订单号"><el-input v-model="form.platformOrderNo" maxlength="100" /></el-form-item>
    <el-form-item label="主播"><el-select v-model="form.anchorId" clearable @change="clearAnchorFee"><el-option v-for="a in anchors" :key="a.id" :label="a.name + (a.remark ? '（' + a.remark + '）' : '')" :value="a.id" :disabled="a.status !== 1 && a.id !== form.anchorId" /></el-select></el-form-item>
    <el-form-item label="入住房间" required><el-select v-model="form.roomId"><el-option v-for="r in rooms" :key="r.id" :label="r.roomNumber" :value="r.id" /></el-select></el-form-item>
    <el-form-item label="入住日期" required><el-date-picker v-model="form.checkInDate" value-format="YYYY-MM-DD" type="date" /></el-form-item><el-form-item label="退房日期" required><el-date-picker v-model="form.checkOutDate" value-format="YYYY-MM-DD" type="date" /></el-form-item>
    <el-form-item label="入住晚数">{{ nights > 0 ? nights : '请选择有效日期' }}</el-form-item><el-form-item label="入住人数"><el-input-number v-model="form.guestCount" :min="1" :precision="0" /></el-form-item>
    <el-form-item label="用户付款"><el-input-number v-model="form.totalAmount" :min="0" :precision="2" /></el-form-item><el-form-item label="平台手续费"><el-input-number v-model="form.platformFee" :min="0" :precision="2" /></el-form-item><el-form-item label="主播手续费"><el-input-number v-model="form.anchorFee" :min="0" :precision="2" :disabled="!form.anchorId" /></el-form-item>
    <el-form-item label="净利润">{{ profit }}</el-form-item><el-form-item label="备注"><el-input v-model="form.remark" maxlength="255" /></el-form-item>
  </el-form><template #footer><el-button @click="visible = false">关闭</el-button><el-button v-if="!form.id || form.status === 'BOOKED'" type="primary" :loading="saving" @click="submit">保存</el-button></template></el-dialog>
</template>
<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPlatforms, getAnchors, getOrders, saveOrder, setOrderStatus } from '@/api/external'
import { getAllRooms } from '@/api/room'
const route = useRoute(), platforms = ref([]), anchors = ref([]), rooms = ref([]), rows = ref([]), total = ref(0), loading = ref(false), saving = ref(false), visible = ref(false), form = ref({})
const filters = ref({ page: 1, size: 10, keyword: '', platformId: null, anchorId: null, roomId: null, status: '' }), dateRange = ref(null)
const labels = { BOOKED: '已预订', CHECKED_IN: '已入住', CHECKED_OUT: '已退房', CANCELLED: '已取消' }
const nights = computed(() => form.value.checkInDate && form.value.checkOutDate ? dayjs(form.value.checkOutDate).diff(dayjs(form.value.checkInDate), 'day') : 0)
const profit = computed(() => ((Number(form.value.totalAmount || 0) * 100 - Number(form.value.platformFee || 0) * 100 - Number(form.value.anchorFee || 0) * 100) / 100).toFixed(2))
async function load() { loading.value = true; try { const result = (await getOrders({ ...filters.value, startDate: dateRange.value?.[0], endDate: dateRange.value?.[1] })).data; rows.value = result.records; total.value = result.total } finally { loading.value = false } }
function search() { filters.value.page = 1; load() }
function create() { form.value = { platformId: null, anchorId: null, roomId: null, checkInDate: '', checkOutDate: '', guestCount: 1, totalAmount: 0, platformFee: 0, anchorFee: 0, remark: '' }; visible.value = true }
function show(row) { form.value = { ...row }; visible.value = true }
function clearAnchorFee() { if (!form.value.anchorId) form.value.anchorFee = 0 }
async function submit() {
  if (!form.value.platformId || !form.value.roomId || nights.value <= 0) return ElMessage.warning('请选择平台、房间和有效住宿日期')
  saving.value = true
  try { await saveOrder(form.value.id, form.value); visible.value = false; ElMessage.success('保存成功'); await load() } finally { saving.value = false }
}
async function change(row, status) { await ElMessageBox.confirm(`确认将订单标记为${labels[status]}？`, '确认'); await setOrderStatus(row.id, status); ElMessage.success('操作成功'); await load() }
onMounted(async () => {
  const [p, a, r] = await Promise.all([getPlatforms({ includeDisabled: true }), getAnchors({ includeDisabled: true }), getAllRooms()]); platforms.value = p.data; anchors.value = a.data; rooms.value = r.data
  await load()
  if (route.query.roomId && route.query.date) { create(); form.value.roomId = Number(route.query.roomId); form.value.checkInDate = route.query.date; form.value.checkOutDate = dayjs(route.query.date).add(1, 'day').format('YYYY-MM-DD') }
  if (route.query.orderId) { const { getOrder } = await import('@/api/external'); show((await getOrder(route.query.orderId)).data) }
})
</script>
<style scoped>.toolbar { display:flex; gap:10px; flex-wrap:wrap; margin-bottom:16px }</style>

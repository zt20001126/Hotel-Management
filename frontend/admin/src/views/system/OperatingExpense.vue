<template>
  <!-- 经营支出管理：统计、筛选和维护独立支出记录。 -->
  <div class="expense-page">
    <div class="page-heading">
      <div><h2>经营支出</h2><p>记录和管理民宿日常经营费用</p></div>
      <el-button type="primary" :icon="Plus" @click="openDialog()">新增支出</el-button>
    </div>
    <div class="stats" v-loading="statsLoading">
      <div class="stat blue"><span>今日支出</span><strong>{{ money(today.totalAmount) }}</strong></div>
      <div class="stat green"><span>本月支出</span><strong>{{ money(monthStats.totalAmount) }}</strong></div>
      <div class="stat purple"><span>本月支出笔数</span><strong>{{ monthStats.totalCount }} 笔</strong></div>
    </div>
    <el-card class="records" shadow="never">
      <div class="toolbar"><h3>支出记录</h3><div class="filters">
        <el-input v-model="filters.keyword" placeholder="搜索支出内容" clearable @keyup.enter="search" />
        <el-date-picker v-model="filters.month" type="month" value-format="YYYY-MM" placeholder="选择月份" :clearable="false" />
        <el-button type="primary" @click="search">查询</el-button>
      </div></div>
      <el-table :data="records" v-loading="loading">
        <el-table-column prop="content" label="支出内容" min-width="260" show-overflow-tooltip />
        <el-table-column label="支出金额" min-width="140"><template #default="{ row }">{{ money(row.amount) }}</template></el-table-column>
        <el-table-column label="支出时间" min-width="190"><template #default="{ row }">{{ formatTime(row.createdAt) }}</template></el-table-column>
        <el-table-column label="操作" width="170"><template #default="{ row }">
          <el-button type="primary" link @click="openDialog(row)">编辑</el-button><el-button type="danger" link @click="remove(row)">删除</el-button>
        </template></el-table-column>
      </el-table>
      <div class="footer"><div>共 {{ pagination.total }} 条记录 · 合计 <strong>{{ money(filteredTotal) }}</strong></div>
        <el-pagination v-model:current-page="pagination.page" v-model:page-size="pagination.size" :page-sizes="[10, 20, 50, 100]" :total="pagination.total" layout="prev, pager, next, sizes" @current-change="loadList" @size-change="changeSize" />
      </div>
    </el-card>
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑支出' : '新增支出'" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="支出内容" prop="content"><el-input v-model="form.content" placeholder="请输入支出内容" maxlength="500" show-word-limit /></el-form-item>
        <el-form-item label="支出金额" prop="amount"><el-input v-model="form.amount" placeholder="请输入支出金额"><template #append>元</template></el-input></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="submitting" @click="submit">{{ editingId ? '保存' : '确认新增' }}</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { getExpenses, getExpenseStatistics, createExpense, updateExpense, deleteExpense } from '@/api/operatingExpense'

const filters = reactive({ keyword: '', month: dayjs().format('YYYY-MM') })
const applied = ref({})
const pagination = reactive({ page: 1, size: 10, total: 0 })
const records = ref([]), filteredTotal = ref(0), loading = ref(false), statsLoading = ref(false)
const today = ref({ totalAmount: 0, totalCount: 0 }), monthStats = ref({ totalAmount: 0, totalCount: 0 })
const dialogVisible = ref(false), submitting = ref(false), editingId = ref(null), formRef = ref()
const form = reactive({ content: '', amount: '' })
const rules = {
  content: [{ required: true, whitespace: true, message: '请输入支出内容', trigger: 'blur' }, { max: 500, message: '最多输入500个字符', trigger: 'blur' }],
  amount: [{ validator: (_rule, value, callback) => /^(?:0|[1-9]\d{0,7})(?:\.\d{1,2})?$/.test(String(value)) && Number(value) > 0 ? callback() : callback(new Error('请输入大于0且最多两位小数的金额')), trigger: 'blur' }]
}
const dateTime = date => date.format('YYYY-MM-DD HH:mm:ss')
const monthRange = month => { const start = dayjs(`${month}-01`); return { startTime: dateTime(start), endTime: dateTime(start.add(1, 'month')) } }
const money = value => `¥${Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
const formatTime = value => value ? value.replace('T', ' ').slice(0, 19) : '-'
function search() { applied.value = { keyword: filters.keyword.trim(), ...monthRange(filters.month) }; pagination.page = 1; loadList() }
async function loadList() {
  loading.value = true
  try {
    const [list, total] = await Promise.all([getExpenses({ ...applied.value, page: pagination.page, size: pagination.size }), getExpenseStatistics(applied.value)])
    if (list.data.records.length === 0 && pagination.page > 1 && list.data.total > 0) { pagination.page = Math.ceil(list.data.total / pagination.size); return await loadList() }
    records.value = list.data.records; pagination.total = list.data.total; filteredTotal.value = total.data.totalAmount
  } finally { loading.value = false }
}
function changeSize() { pagination.page = 1; loadList() }
async function loadStats() {
  statsLoading.value = true
  try {
    const start = dayjs().startOf('day')
    const [daily, monthly] = await Promise.all([getExpenseStatistics({ startTime: dateTime(start), endTime: dateTime(start.add(1, 'day')) }), getExpenseStatistics(monthRange(dayjs().format('YYYY-MM')))])
    today.value = daily.data; monthStats.value = monthly.data
  } finally { statsLoading.value = false }
}
function openDialog(row) { editingId.value = row?.id || null; form.content = row?.content || ''; form.amount = row ? String(row.amount) : ''; dialogVisible.value = true; formRef.value?.clearValidate() }
async function refresh() { await Promise.all([loadList(), loadStats()]) }
async function submit() {
  if (!await formRef.value.validate().catch(() => false)) return
  submitting.value = true
  try {
    const payload = { content: form.content.trim(), amount: form.amount }
    if (editingId.value) await updateExpense(editingId.value, payload); else await createExpense(payload)
    dialogVisible.value = false; ElMessage.success('保存成功'); await refresh()
  } finally { submitting.value = false }
}
async function remove(row) {
  try { await ElMessageBox.confirm('确定删除这条经营支出记录吗？删除后无法恢复。', '删除支出', { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }) } catch { return }
  await deleteExpense(row.id); ElMessage.success('删除成功'); await refresh()
}
onMounted(() => { search(); loadStats() })
</script>

<style scoped>
.expense-page { color: #18263e; }
.page-heading, .toolbar, .footer { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
h2 { margin: 0 0 8px; font-size: 26px; } h3 { margin: 0; font-size: 20px; } p { margin: 0; color: #69758a; }
.stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin: 24px 0; }
.stat { padding: 24px; border-radius: 10px; display: flex; flex-direction: column; gap: 12px; }
.stat strong { font-size: 28px; } .blue { background: #e6f0ff; } .green { background: #e5f5ee; } .purple { background: #eeeafd; }
.records { border-radius: 10px; } .toolbar { margin-bottom: 20px; flex-wrap: wrap; } .filters { display: flex; gap: 12px; flex-wrap: wrap; }
.filters .el-input { width: 230px; } .footer { margin-top: 24px; flex-wrap: wrap; color: #69758a; } .footer strong { color: #2866e9; font-size: 20px; }
@media (max-width: 768px) { .stats { grid-template-columns: 1fr; } .page-heading { align-items: flex-start; } }
</style>

<!-- 维护第三方平台和主播；停用后保留历史记录。 -->
<template>
  <el-card><el-tabs v-model="tab" @tab-change="load"><el-tab-pane label="平台" name="platform" /><el-tab-pane label="主播" name="anchor" /></el-tabs>
    <el-button type="primary" @click="edit()">新增{{ tab === 'platform' ? '平台' : '主播' }}</el-button>
    <el-table :data="items" v-loading="loading"><el-table-column prop="name" label="名称" /><el-table-column v-if="tab === 'anchor'" prop="remark" label="备注" /><el-table-column label="状态"><template #default="{ row }">{{ row.status === 1 ? '启用' : '停用' }}</template></el-table-column><el-table-column label="操作"><template #default="{ row }"><el-button link type="primary" @click="edit(row)">编辑</el-button></template></el-table-column></el-table>
  </el-card>
  <el-dialog v-model="visible" :title="form.id ? '编辑' : '新增'" width="480px"><el-form label-width="80px"><el-form-item label="名称" required><el-input v-model="form.name" maxlength="50" /></el-form-item><el-form-item v-if="tab === 'anchor'" label="备注"><el-input v-model="form.remark" maxlength="255" /></el-form-item><el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" /></el-form-item></el-form><template #footer><el-button @click="visible = false">取消</el-button><el-button type="primary" :loading="saving" @click="submit">保存</el-button></template></el-dialog>
</template>
<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getPlatforms, getAnchors, savePlatform, saveAnchor } from '@/api/external'
const tab = ref('platform'), items = ref([]), loading = ref(false), saving = ref(false), visible = ref(false), form = ref({})
async function load() { loading.value = true; try { items.value = (await (tab.value === 'platform' ? getPlatforms : getAnchors)({ includeDisabled: true })).data } finally { loading.value = false } }
function edit(row) { form.value = row ? { ...row } : { name: '', status: 1, remark: '' }; visible.value = true }
async function submit() { if (!form.value.name?.trim()) return ElMessage.warning('请输入名称'); saving.value = true; try { await (tab.value === 'platform' ? savePlatform : saveAnchor)(form.value.id, form.value); visible.value = false; ElMessage.success('保存成功'); await load() } finally { saving.value = false } }
load()
</script>

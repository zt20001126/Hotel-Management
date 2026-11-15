<!-- 汇总第三方订单经营数据，不包含小程序订单。 -->
<template>
  <el-alert title="只统计已入住和已退房的第三方订单，按计划住宿日期分摊；不含小程序及经营支出" type="info" :closable="false" />
  <el-card style="margin-top:16px"><div class="filters"><el-date-picker v-model="month" type="month" value-format="YYYY-MM" :clearable="false" /><el-select v-model="platformId" clearable placeholder="全部平台"><el-option v-for="p in platforms" :key="p.id" :label="p.name" :value="p.id" /></el-select><el-select v-model="anchorId" clearable placeholder="全部主播"><el-option v-for="a in anchors" :key="a.id" :label="a.name" :value="a.id" /></el-select><el-select v-model="roomId" clearable placeholder="全部房间"><el-option v-for="r in rooms" :key="r.id" :label="r.roomNumber" :value="r.id" /></el-select><el-button type="primary" @click="load">查询</el-button></div>
    <div v-if="data" class="summary"><div v-for="column in columns.slice(1)" :key="column.key">{{ column.label }}<strong>{{ data.total[column.key] }}</strong></div></div>
    <el-tabs v-model="tab"><el-tab-pane label="每日明细" name="daily" /><el-tab-pane label="平台统计" name="platforms" /><el-tab-pane label="主播统计" name="anchors" /><el-tab-pane label="房间统计" name="rooms" /></el-tabs>
    <el-table v-loading="loading" :data="data?.[tab] || []"><el-table-column v-for="c in columns" :key="c.key" :prop="c.key" :label="c.label" /></el-table>
  </el-card>
</template>
<script setup>
import { ref } from 'vue'
import dayjs from 'dayjs'
import { getPlatforms, getAnchors, getStatistics } from '@/api/external'
import { getAllRooms } from '@/api/room'
const month = ref(dayjs().format('YYYY-MM')), platformId = ref(null), anchorId = ref(null), roomId = ref(null), platforms = ref([]), anchors = ref([]), rooms = ref([]), data = ref(null), loading = ref(false), tab = ref('daily')
const columns = [{key:'name',label:'日期/名称'}, {key:'orderCount',label:'订单数'}, {key:'nights',label:'间夜'}, {key:'totalAmount',label:'营业额'}, {key:'platformFee',label:'平台手续费'}, {key:'anchorFee',label:'主播手续费'}, {key:'netProfit',label:'净利润'}]
async function load() { loading.value = true; data.value = null; try { const date = dayjs(month.value + '-01'); data.value = (await getStatistics({ startDate: date.format('YYYY-MM-DD'), endDate: date.endOf('month').format('YYYY-MM-DD'), platformId: platformId.value || undefined, anchorId: anchorId.value || undefined, roomId: roomId.value || undefined })).data } finally { loading.value = false } }
getPlatforms({ includeDisabled: true }).then(res => { platforms.value = res.data }); getAnchors({ includeDisabled: true }).then(res => { anchors.value = res.data }); getAllRooms().then(res => { rooms.value = res.data }); load()
</script>
<style scoped>.filters,.summary { display:flex; gap:16px; flex-wrap:wrap; margin-bottom:20px }.summary div { flex:1; min-width:120px; background:#f5f7fa; padding:15px }.summary strong { display:block; margin-top:8px; font-size:20px }</style>

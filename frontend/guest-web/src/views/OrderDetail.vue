<template>
  <div class="order-detail-page">
    <!-- 头部导航 -->
    <el-header class="header">
      <div class="guest-container">
        <div class="header-content">
          <div class="logo" @click="$router.push('/')">
            <h2>芊浔民宿</h2>
          </div>
          <el-menu
            mode="horizontal"
            :default-active="activeMenu"
            router
            class="header-menu"
          >
            <el-menu-item index="/">首页</el-menu-item>
            <el-menu-item index="/rooms">房源查询</el-menu-item>
            <el-menu-item index="/orders" v-if="userStore.isLoggedIn">我的订单</el-menu-item>
            <el-menu-item index="/profile" v-if="userStore.isLoggedIn">个人中心</el-menu-item>
          </el-menu>
          <div class="header-actions">
            <span class="welcome-text">欢迎，{{ userStore.realName || userStore.username }}</span>
            <el-button type="text" @click="handleLogout">退出</el-button>
          </div>
        </div>
      </div>
    </el-header>

    <div class="order-detail-content" v-loading="loading">
      <div class="guest-container">
        <el-button
          type="text"
          @click="$router.push('/orders')"
          style="margin-bottom: 20px"
        >
          <el-icon><ArrowLeft /></el-icon>
          返回订单列表
        </el-button>

        <el-card v-if="order" class="order-detail-card">
          <template #header>
            <div class="card-header">
              <div>
                <h3>订单详情</h3>
                <span class="order-no">订单号：{{ order.orderNo }}</span>
              </div>
              <el-tag :type="getStatusType(order.status)" size="large">
                {{ getStatusText(order.status) }}
              </el-tag>
            </div>
          </template>

          <div class="detail-sections">
            <!-- 订单信息 -->
            <div class="detail-section">
              <h4>订单信息</h4>
              <el-descriptions :column="2" border>
                <el-descriptions-item label="订单号">{{ order.orderNo }}</el-descriptions-item>
                <el-descriptions-item label="订单状态">
                  <el-tag :type="getStatusType(order.status)">
                    {{ getStatusText(order.status) }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="下单时间">{{ formatDateTime(order.createTime) }}</el-descriptions-item>
                <el-descriptions-item label="更新时间">{{ formatDateTime(order.updateTime) }}</el-descriptions-item>
              </el-descriptions>
            </div>

            <!-- 房型信息 -->
            <div class="detail-section">
              <h4>房型信息</h4>
              <el-descriptions :column="2" border>
                <el-descriptions-item label="房型名称">{{ order.roomTypeName || '--' }}</el-descriptions-item>
                <el-descriptions-item label="房间号" v-if="order.roomNo">
                  {{ order.roomNo }}
                </el-descriptions-item>
                <el-descriptions-item label="入住日期">{{ order.checkInDate }}</el-descriptions-item>
                <el-descriptions-item label="退房日期">{{ order.checkOutDate }}</el-descriptions-item>
                <el-descriptions-item label="入住人数">{{ order.guestCount }} 人</el-descriptions-item>
                <el-descriptions-item label="入住天数">{{ nights }} 晚</el-descriptions-item>
              </el-descriptions>
            </div>

            <!-- 联系人信息 -->
            <div class="detail-section">
              <h4>联系人信息</h4>
              <el-descriptions :column="2" border>
                <el-descriptions-item label="联系人姓名">{{ order.guestName }}</el-descriptions-item>
                <el-descriptions-item label="联系电话">{{ order.guestPhone }}</el-descriptions-item>
                <el-descriptions-item label="身份证号" v-if="order.guestIdCard">
                  {{ order.guestIdCard }}
                </el-descriptions-item>
                <el-descriptions-item label="备注" :span="2" v-if="order.remark">
                  {{ order.remark }}
                </el-descriptions-item>
              </el-descriptions>
            </div>

            <!-- 价格信息 -->
            <div class="detail-section">
              <h4>价格信息</h4>
              <el-descriptions :column="2" border>
                <el-descriptions-item label="单价">¥{{ Number(order.roomPrice || order.totalPrice / nights || 0).toFixed(2) }} /晚</el-descriptions-item>
                <el-descriptions-item label="入住天数">{{ nights }} 晚</el-descriptions-item>
                <el-descriptions-item label="总价">
                  <span class="price-text">¥{{ Number(order.totalPrice || 0).toFixed(2) }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="保证金">
                  <span class="deposit-text">¥{{ Number(order.deposit || 0).toFixed(2) }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="已支付">
                  <span class="paid-text">¥{{ Number(order.paidAmount || 0).toFixed(2) }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="待支付">
                  <span class="unpaid-text">¥{{ unpaidAmount.toFixed(2) }}</span>
                </el-descriptions-item>
              </el-descriptions>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="action-buttons" v-if="canShowActions">
            <el-button
              v-if="unpaidAmount > 0"
              type="primary"
              size="large"
              @click="openPaymentDialog"
            >
              立即支付
            </el-button>
            <el-button
              v-if="canModify"
              type="warning"
              size="large"
              @click="openModifyDialog"
            >
              改订
            </el-button>
            <el-button
              v-if="canCancel"
              type="danger"
              size="large"
              @click="handleCancel"
            >
              取消订单
            </el-button>
          </div>
        </el-card>

        <div v-else-if="!loading" class="empty-state">
          <div class="empty-icon">📋</div>
          <div class="empty-text">订单不存在</div>
          <el-button type="primary" @click="$router.push('/orders')">返回订单列表</el-button>
        </div>
      </div>
    </div>

    <!-- 支付对话框 -->
    <el-dialog
      v-model="showPaymentDialog"
      title="支付订单"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="paymentForm" label-width="100px">
        <el-form-item label="订单号">
          <span>{{ order?.orderNo }}</span>
        </el-form-item>
        <el-form-item label="支付类型" required>
          <el-radio-group v-model="paymentForm.paymentType">
            <el-radio label="deposit" :disabled="hasPaidDeposit || remainingDeposit <= 0">
              支付保证金 (¥{{ remainingDeposit }})
            </el-radio>
            <el-radio label="room_fee" :disabled="hasPaidRoomFee || remainingRoomFee <= 0">
              支付房费 (¥{{ remainingRoomFee }})
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="支付金额">
          <span class="payment-amount">¥{{ paymentAmount }}</span>
        </el-form-item>
        <el-form-item label="支付方式" required>
          <el-radio-group v-model="paymentForm.paymentMethod">
            <el-radio label="wechat">微信支付</el-radio>
            <el-radio label="alipay">支付宝</el-radio>
            <el-radio label="card">银行卡</el-radio>
            <el-radio label="cash">现金</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="paymentForm.remark"
            type="textarea"
            :rows="3"
            placeholder="可选"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPaymentDialog = false">取消</el-button>
        <el-button type="primary" @click="handlePayment" :loading="paymentLoading">
          确认支付
        </el-button>
      </template>
    </el-dialog>

    <!-- 改订对话框 -->
    <el-dialog
      v-model="showModifyDialog"
      title="改订订单"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="modifyForm" label-width="100px">
        <el-form-item label="原入住日期">
          <span>{{ order?.checkInDate }}</span>
        </el-form-item>
        <el-form-item label="原退房日期">
          <span>{{ order?.checkOutDate }}</span>
        </el-form-item>
        <el-form-item label="新入住日期" required>
          <el-date-picker
            v-model="modifyForm.checkInDate"
            type="date"
            placeholder="选择新入住日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            :disabled-date="disabledModifyCheckInDate"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="新退房日期" required>
          <el-date-picker
            v-model="modifyForm.checkOutDate"
            type="date"
            placeholder="选择新退房日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            :disabled-date="disabledModifyCheckOutDate"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="新入住天数">
          <span>{{ modifyNights }} 晚</span>
        </el-form-item>
        <el-form-item label="新房费">
          <span class="price-text">¥{{ modifyTotalPrice }}</span>
        </el-form-item>
        <el-form-item label="新保证金">
          <span class="deposit-text">¥{{ modifyDeposit }}</span>
        </el-form-item>
        <el-form-item label="价格差额">
          <span :class="modifyPriceDiff >= 0 ? 'unpaid-text' : 'paid-text'">
            {{ modifyPriceDiff >= 0 ? '+' : '' }}¥{{ modifyPriceDiff }}
          </span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showModifyDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmModify" :loading="modifyLoading">
          确认改订
        </el-button>
      </template>
    </el-dialog>

    <!-- 页脚 -->
    <el-footer class="footer">
      <div class="guest-container">
        <p>&copy; 2025 芊浔民宿. 保留所有权利.</p>
      </div>
    </el-footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getOrder, cancelOrder, updateOrder } from '@/api/order'
import { getDepositRate } from '@/api/room'
import { createPayment, getOrderPayments } from '@/api/payment'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)
const loading = ref(false)
const order = ref(null)
const payments = ref([])  // 支付记录
const showPaymentDialog = ref(false)
const paymentLoading = ref(false)
const paymentForm = ref({
  paymentType: 'deposit',
  paymentMethod: 'wechat',
  remark: ''
})

// 改订相关
const showModifyDialog = ref(false)
const modifyLoading = ref(false)
const depositRate = ref(0.3)
const modifyForm = ref({
  checkInDate: '',
  checkOutDate: ''
})

const nights = computed(() => {
  if (!order.value?.checkInDate || !order.value?.checkOutDate) {
    return 0
  }
  const checkIn = dayjs(order.value.checkInDate)
  const checkOut = dayjs(order.value.checkOutDate)
  return checkOut.diff(checkIn, 'day')
})

const unpaidAmount = computed(() => {
  if (!order.value) return 0
  const total = Number(order.value.totalPrice || 0)
  const deposit = Number(order.value.deposit || 0)
  const extraCharges = Number(order.value.extraCharges || 0)
  const paid = Number(order.value.paidAmount || 0)
  // 保留两位小数，避免浮点数误差
  return Math.max(0, parseFloat((deposit + total + extraCharges - paid).toFixed(2)))
})

// 检查是否已支付保证金
const hasPaidDeposit = computed(() => {
  return payments.value.some(p => p.paymentType === 'deposit' && p.status === 'success')
})

// 检查是否已支付房费
const hasPaidRoomFee = computed(() => {
  return payments.value.some(p => p.paymentType === 'room_fee' && p.status === 'success')
})

// 剩余保证金
const remainingDeposit = computed(() => {
  if (!order.value) return 0
  const deposit = Number(order.value.deposit || 0)
  const paidDeposit = payments.value
    .filter(p => p.paymentType === 'deposit' && p.status === 'success')
    .reduce((sum, p) => sum + Number(p.amount || 0), 0)
  return Math.max(0, parseFloat((deposit - paidDeposit).toFixed(2)))
})

// 剩余房费
const remainingRoomFee = computed(() => {
  if (!order.value) return 0
  const roomFee = Number(order.value.totalPrice || 0)
  const extraCharges = Number(order.value.extraCharges || 0)
  const paidRoomFee = payments.value
    .filter(p => p.paymentType === 'room_fee' && p.status === 'success')
    .reduce((sum, p) => sum + Number(p.amount || 0), 0)
  return Math.max(0, parseFloat((roomFee + extraCharges - paidRoomFee).toFixed(2)))
})

// 是否已改订过
const isModified = computed(() => {
  return order.value?.modifyCount && order.value.modifyCount >= 1
})

// 改订相关计算
const modifyNights = computed(() => {
  if (!modifyForm.value.checkInDate || !modifyForm.value.checkOutDate) return 0
  const checkIn = dayjs(modifyForm.value.checkInDate)
  const checkOut = dayjs(modifyForm.value.checkOutDate)
  return checkOut.diff(checkIn, 'day')
})

const modifyTotalPrice = computed(() => {
  if (!order.value || modifyNights.value <= 0) return 0
  const roomPrice = Number(order.value.roomPrice || order.value.totalPrice / nights.value || 0)
  return parseFloat((roomPrice * modifyNights.value).toFixed(2))
})

const modifyDeposit = computed(() => {
  return parseFloat((modifyTotalPrice.value * depositRate.value).toFixed(2))
})

const modifyPriceDiff = computed(() => {
  if (!order.value) return 0
  const originalTotal = Number(order.value.totalPrice || 0) + Number(order.value.deposit || 0)
  const newTotal = modifyTotalPrice.value + modifyDeposit.value
  return parseFloat((newTotal - originalTotal).toFixed(2))
})

// 是否可以显示操作按钮（待确认、已确认、已入住、已退房状态都可能需要操作）
const canShowActions = computed(() => {
  const status = (order.value?.status || '').toLowerCase()
  // 待确认、已确认可以支付/改订/取消
  // 已入住可以支付
  // 已退房可以支付剩余费用
  return ['pending', 'confirmed', 'checked_in', 'checked_out'].includes(status)
})

// 是否可以改订（只有待确认或已确认且已付清且未改订过）
const canModify = computed(() => {
  const status = (order.value?.status || '').toLowerCase()
  return ['pending', 'confirmed'].includes(status) && !isModified.value && unpaidAmount.value <= 0
})

// 是否可以取消订单（只有待确认或已确认状态）
const canCancel = computed(() => {
  const status = (order.value?.status || '').toLowerCase()
  return ['pending', 'confirmed'].includes(status)
})

const paymentAmount = computed(() => {
  if (!order.value) return 0
  if (paymentForm.value.paymentType === 'deposit') {
    return remainingDeposit.value
  } else {
    return remainingRoomFee.value
  }
})

const getStatusType = (status) => {
  const statusMap = {
    PENDING: 'warning',
    pending: 'warning',
    CONFIRMED: 'success',
    confirmed: 'success',
    CHECKED_IN: 'primary',
    checked_in: 'primary',
    CHECKED_OUT: 'info',
    checked_out: 'info',
    CANCELLED: 'danger',
    cancelled: 'danger'
  }
  return statusMap[status] || 'info'
}

const getStatusText = (status) => {
  const statusMap = {
    PENDING: '待确认',
    pending: '待确认',
    CONFIRMED: '已确认',
    confirmed: '已确认',
    CHECKED_IN: '已入住',
    checked_in: '已入住',
    CHECKED_OUT: '已退房',
    checked_out: '已退房',
    CANCELLED: '已取消',
    cancelled: '已取消'
  }
  return statusMap[status] || status
}

const formatDateTime = (dateTime) => {
  if (!dateTime) return ''
  return dateTime.replace('T', ' ')
}

const loadOrder = async () => {
  loading.value = true
  try {
    const res = await getOrder(route.params.id)
    order.value = res.data
    // 加载支付记录
    await loadPayments()
  } catch (error) {
    console.error('加载订单详情失败:', error)
    ElMessage.error('加载订单详情失败')
    router.push('/orders')
  } finally {
    loading.value = false
  }
}

const loadPayments = async () => {
  try {
    const res = await getOrderPayments(route.params.id)
    payments.value = res.data || []
  } catch (error) {
    console.error('加载支付记录失败:', error)
    payments.value = []
  }
}

const handleCancel = async () => {
  try {
    await ElMessageBox.confirm('确定要取消这个订单吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await cancelOrder(route.params.id)
    ElMessage.success('订单已取消')
    loadOrder()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消订单失败:', error)
    }
  }
}

// 打开支付对话框，自动选择未支付的类型
const openPaymentDialog = () => {
  // 根据已支付情况自动选择支付类型
  if (!hasPaidDeposit.value && remainingDeposit.value > 0) {
    paymentForm.value.paymentType = 'deposit'
  } else if (!hasPaidRoomFee.value && remainingRoomFee.value > 0) {
    paymentForm.value.paymentType = 'room_fee'
  } else {
    // 都已支付或无需支付
    ElMessage.info('无需支付')
    return
  }
  paymentForm.value.paymentMethod = 'wechat'
  paymentForm.value.remark = ''
  showPaymentDialog.value = true
}

const handlePayment = async () => {
  if (!paymentForm.value.paymentMethod) {
    ElMessage.warning('请选择支付方式')
    return
  }

  // 防止重复支付
  if (paymentForm.value.paymentType === 'deposit' && hasPaidDeposit.value) {
    ElMessage.warning('保证金已支付，不能重复支付')
    return
  }
  if (paymentForm.value.paymentType === 'room_fee' && hasPaidRoomFee.value) {
    ElMessage.warning('房费已支付，不能重复支付')
    return
  }

  const amount = paymentAmount.value
  if (amount <= 0) {
    ElMessage.info('无需支付')
    return
  }

  paymentLoading.value = true
  try {
    await createPayment({
      orderId: order.value.id,
      amount: amount,
      paymentType: paymentForm.value.paymentType,
      paymentMethod: paymentForm.value.paymentMethod,
      remark: paymentForm.value.remark
    })
    
    ElMessage.success('支付成功！')
    showPaymentDialog.value = false
    // 重置表单
    paymentForm.value = {
      paymentType: 'deposit',
      paymentMethod: 'wechat',
      remark: ''
    }
    // 重新加载订单信息
    await loadOrder()
  } catch (error) {
    console.error('支付失败:', error)
    ElMessage.error(error.response?.data?.message || '支付失败，请重试')
  } finally {
    paymentLoading.value = false
  }
}

const handleLogout = () => {
  userStore.logout()
  ElMessage.success('已退出登录')
}

// 改订相关函数
const loadDepositRate = async () => {
  try {
    const res = await getDepositRate()
    if (res.data) {
      depositRate.value = parseFloat(res.data)
    }
  } catch (error) {
    console.error('加载保证金比例失败:', error)
  }
}

const disabledModifyCheckInDate = (date) => {
  return date < new Date(new Date().setHours(0, 0, 0, 0))
}

const disabledModifyCheckOutDate = (date) => {
  if (!modifyForm.value.checkInDate) {
    return date < new Date(new Date().setHours(0, 0, 0, 0))
  }
  return date <= new Date(modifyForm.value.checkInDate)
}

const openModifyDialog = () => {
  if (isModified.value) {
    ElMessage.warning('该订单已改订过，每个订单只能改订一次')
    return
  }
  modifyForm.value = {
    checkInDate: order.value?.checkInDate || '',
    checkOutDate: order.value?.checkOutDate || ''
  }
  showModifyDialog.value = true
}

const confirmModify = async () => {
  if (!modifyForm.value.checkInDate || !modifyForm.value.checkOutDate) {
    ElMessage.warning('请选择入住和退房日期')
    return
  }
  if (modifyNights.value <= 0) {
    ElMessage.warning('退房日期必须晚于入住日期')
    return
  }

  modifyLoading.value = true
  try {
    await updateOrder({
      id: order.value.id,
      checkInDate: modifyForm.value.checkInDate,
      checkOutDate: modifyForm.value.checkOutDate,
      nights: modifyNights.value,
      totalPrice: modifyTotalPrice.value,
      deposit: modifyDeposit.value
    })
    ElMessage.success('改订成功')
    showModifyDialog.value = false
    await loadOrder()
  } catch (error) {
    console.error('改订失败:', error)
    ElMessage.error(error.response?.data?.message || '改订失败，请重试')
  } finally {
    modifyLoading.value = false
  }
}

onMounted(() => {
  loadOrder()
  loadDepositRate()
})
</script>

<style scoped lang="scss">
.order-detail-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.header {
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 0;
  height: 70px;
  line-height: 70px;

  .header-content {
    display: flex;
    align-items: center;
    justify-content: space-between;

    .logo {
      cursor: pointer;
      h2 {
        margin: 0;
        color: #667eea;
        font-size: 24px;
      }
    }

    .header-menu {
      flex: 1;
      border: none;
      margin-left: 40px;
    }

    .header-actions {
      display: flex;
      align-items: center;
      gap: 15px;

      .welcome-text {
        color: #666;
        font-size: 14px;
      }
    }
  }
}

.order-detail-content {
  flex: 1;
  padding: 40px 0;
  background: #f5f7fa;

  .order-detail-card {
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      h3 {
        margin: 0 0 5px 0;
        color: #333;
      }

      .order-no {
        color: #999;
        font-size: 14px;
      }
    }

    .detail-sections {
      .detail-section {
        margin-bottom: 30px;

        h4 {
          font-size: 18px;
          margin-bottom: 15px;
          color: #333;
        }

        .price-text {
          font-size: 20px;
          font-weight: 600;
          color: #667eea;
        }

        .deposit-text {
          font-size: 18px;
          font-weight: 600;
          color: #f56c6c;
        }

        .paid-text {
          font-size: 18px;
          font-weight: 600;
          color: #67c23a;
        }

        .unpaid-text {
          font-size: 18px;
          font-weight: 600;
          color: #e6a23c;
        }
      }
    }

    .action-buttons {
      margin-top: 30px;
      text-align: right;
      display: flex;
      gap: 15px;
      justify-content: flex-end;
    }
  }
}

.footer {
  background: #2c3e50;
  color: white;
  text-align: center;
  line-height: 60px;
  margin-top: 60px;
}

.payment-amount {
  font-size: 24px;
  font-weight: 600;
  color: #667eea;
}
</style>


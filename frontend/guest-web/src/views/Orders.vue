<template>
  <div class="orders-page">
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

    <div class="orders-content">
      <div class="guest-container">
        <h2 class="page-title">我的订单</h2>

        <!-- 筛选标签 -->
        <div class="filter-tabs">
          <el-radio-group v-model="filterStatus" @change="loadOrders">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button label="PENDING">待确认</el-radio-button>
            <el-radio-button label="CONFIRMED">已确认</el-radio-button>
            <el-radio-button label="CHECKED_IN">已入住</el-radio-button>
            <el-radio-button label="CHECKED_OUT">已退房</el-radio-button>
            <el-radio-button label="CANCELLED">已取消</el-radio-button>
          </el-radio-group>
        </div>

        <!-- 订单列表 -->
        <div v-loading="loading" class="orders-list">
          <el-card
            v-for="order in orders"
            :key="order.id"
            class="order-card"
            shadow="hover"
          >
            <div class="order-header">
              <div class="order-info">
                <span class="order-no">订单号：{{ order.orderNo }}</span>
                <el-tag :type="getStatusType(order.status)" class="status-tag">
                  {{ getStatusText(order.status) }}
                </el-tag>
              </div>
              <div class="order-actions">
                <el-button type="primary" text @click="goToOrderDetail(order.id)">
                  查看详情
                </el-button>
                <el-button
                  v-if="canPay(order)"
                  type="success"
                  text
                  @click="handlePayment(order)"
                >
                  立即支付
                </el-button>
                <el-button
                  v-if="order.status === 'PENDING' || order.status === 'pending' || order.status === 'CONFIRMED' || order.status === 'confirmed'"
                  type="danger"
                  text
                  @click="handleCancel(order.id)"
                >
                  取消订单
                </el-button>
              </div>
            </div>
            <div class="order-body">
              <div class="order-item">
                <div class="item-label">房型：</div>
                <div class="item-value">{{ order.roomTypeName || '--' }}</div>
              </div>
              <div class="order-item">
                <div class="item-label">入住日期：</div>
                <div class="item-value">{{ order.checkInDate }}</div>
              </div>
              <div class="order-item">
                <div class="item-label">退房日期：</div>
                <div class="item-value">{{ order.checkOutDate }}</div>
              </div>
              <div class="order-item">
                <div class="item-label">入住人数：</div>
                <div class="item-value">{{ order.guestCount }} 人</div>
              </div>
              <div class="order-item">
                <div class="item-label">联系人：</div>
                <div class="item-value">{{ order.guestName }}</div>
              </div>
              <div class="order-item">
                <div class="item-label">联系电话：</div>
                <div class="item-value">{{ order.guestPhone }}</div>
              </div>
            </div>
            <div class="order-footer">
              <div class="price-info">
                <span class="total-price">总价：¥{{ order.totalPrice }}</span>
                <span class="deposit" v-if="order.deposit">保证金：¥{{ order.deposit }}</span>
                <span class="paid" v-if="order.paidAmount > 0">已支付：¥{{ order.paidAmount }}</span>
                <span class="unpaid" v-if="getUnpaidAmount(order) > 0">待支付：¥{{ getUnpaidAmount(order) }}</span>
              </div>
              <div class="order-time">
                下单时间：{{ formatDateTime(order.createTime) }}
              </div>
            </div>
          </el-card>

          <div v-if="!loading && orders.length === 0" class="empty-state">
            <div class="empty-icon">📋</div>
            <div class="empty-text">暂无订单</div>
            <el-button type="primary" @click="$router.push('/rooms')">去预订</el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 页脚 -->
    <el-footer class="footer">
      <div class="guest-container">
        <p>&copy; 2025 芊浔民宿. 保留所有权利.</p>
      </div>
    </el-footer>

    <!-- 支付对话框 -->
    <el-dialog
      v-model="showPaymentDialog"
      title="支付订单"
      width="500px"
      :close-on-click-modal="false"
      @close="handlePaymentDialogClose"
    >
      <el-form :model="paymentForm" label-width="100px" v-if="currentOrder">
        <el-form-item label="订单号">
          <span>{{ currentOrder.orderNo }}</span>
        </el-form-item>
        <el-form-item label="支付类型" required>
          <el-radio-group v-model="paymentForm.paymentType">
            <el-radio
              label="deposit"
              :disabled="hasPaidDeposit(currentOrderPayments) || getRemainingDeposit(currentOrder, currentOrderPayments) <= 0"
            >
              支付保证金 (¥{{ getRemainingDeposit(currentOrder, currentOrderPayments) }})
            </el-radio>
            <el-radio
              label="room_fee"
              :disabled="hasPaidRoomFee(currentOrderPayments) || getUnpaidRoomFee(currentOrder, currentOrderPayments) <= 0"
            >
              支付房费 (¥{{ getUnpaidRoomFee(currentOrder, currentOrderPayments) }})
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="支付金额">
          <span class="payment-amount">¥{{ getPaymentAmount() }}</span>
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
        <el-button @click="handlePaymentDialogClose">取消</el-button>
        <el-button type="primary" @click="confirmPayment" :loading="paymentLoading">
          确认支付
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getMyOrders, cancelOrder } from '@/api/order'
import { createPayment, getOrderPayments } from '@/api/payment'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)
const loading = ref(false)
const filterStatus = ref('')
const orders = ref([])
const showPaymentDialog = ref(false)
const paymentLoading = ref(false)
const currentOrder = ref(null)
const currentOrderPayments = ref([])
const paymentForm = ref({
  paymentType: 'deposit',
  paymentMethod: 'wechat',
  remark: ''
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

const formatDateTime = (dateTime) => {
  if (!dateTime) return ''
  return dateTime.replace('T', ' ')
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

const loadOrders = async () => {
  loading.value = true
  try {
    const res = await getMyOrders()
    let orderList = res.data || []

    if (filterStatus.value) {
      // 状态筛选（兼容大小写和下划线格式）
      const targetStatus = filterStatus.value.toLowerCase().replace(/_/g, '_')
      orderList = orderList.filter(order => {
        const orderStatus = (order.status || '').toLowerCase().trim()
        return orderStatus === targetStatus
      })
    }

    orders.value = orderList
  } catch (error) {
    console.error('加载订单失败:', error)
    ElMessage.error('加载订单失败')
  } finally {
    loading.value = false
  }
}

const goToOrderDetail = (id) => {
  router.push(`/order/${id}`)
}

// 获取待支付金额（用于列表显示：保证金 + 房费 + 额外消费 - 已付）
const getUnpaidAmount = (order) => {
  if (!order) return 0
  const deposit = Number(order.deposit || 0)
  const roomFee = Number(order.totalPrice || 0)
  const extraCharges = Number(order.extraCharges || 0)
  const paid = Number(order.paidAmount || 0)
  const totalDue = parseFloat((deposit + roomFee + extraCharges).toFixed(2))
  return Math.max(0, parseFloat((totalDue - paid).toFixed(2)))
}

// 获取待支付房费（用于支付对话框，基于支付记录精确计算）
const getUnpaidRoomFee = (order, payments = []) => {
  if (!order) return 0
  const roomFee = Number(order.totalPrice || 0)
  const extraCharges = Number(order.extraCharges || 0)
  // 计算已支付的房费总额
  const paidRoomFee = payments
    .filter(p => p.paymentType === 'room_fee' && p.status === 'success')
    .reduce((sum, p) => sum + Number(p.amount || 0), 0)
  const totalDue = parseFloat((roomFee + extraCharges).toFixed(2))
  return Math.max(0, parseFloat((totalDue - paidRoomFee).toFixed(2)))
}

// 获取剩余保证金（用于列表显示，使用简单计算）
const getDepositRemaining = (order) => {
  if (!order) return 0
  const deposit = Number(order.deposit || 0)
  const paid = Number(order.paidAmount || 0)
  // 简单计算：如果已支付金额小于保证金，则还有剩余
  if (paid < deposit) {
    return Math.max(0, parseFloat((deposit - paid).toFixed(2)))
  }
  return 0
}

// 获取剩余保证金（用于支付对话框，基于支付记录精确计算）
const getRemainingDeposit = (order, payments = []) => {
  if (!order) return 0
  const deposit = Number(order.deposit || 0)
  // 计算已支付的保证金总额
  const paidDeposit = payments
    .filter(p => p.paymentType === 'deposit' && p.status === 'success')
    .reduce((sum, p) => sum + Number(p.amount || 0), 0)
  return Math.max(0, parseFloat((deposit - paidDeposit).toFixed(2)))
}

// 检查是否已支付保证金
const hasPaidDeposit = (payments = []) => {
  return payments.some(
    p => p.paymentType === 'deposit' && p.status === 'success'
  )
}

// 检查是否已支付房费
const hasPaidRoomFee = (payments = []) => {
  return payments.some(
    p => p.paymentType === 'room_fee' && p.status === 'success'
  )
}

const canPay = (order) => {
  if (order.status !== 'PENDING' && order.status !== 'pending' && 
      order.status !== 'CONFIRMED' && order.status !== 'confirmed') {
    return false
  }
  return getDepositRemaining(order) > 0 || getUnpaidAmount(order) > 0
}

const getPaymentAmount = () => {
  if (!currentOrder.value) return 0
  if (paymentForm.value.paymentType === 'deposit') {
    return getRemainingDeposit(currentOrder.value, currentOrderPayments.value)
  } else {
    return getUnpaidRoomFee(currentOrder.value, currentOrderPayments.value)
  }
}

const handlePayment = async (order) => {
  currentOrder.value = order
  // 获取订单的支付记录
  try {
    const res = await getOrderPayments(order.id)
    currentOrderPayments.value = res.data || []
  } catch (error) {
    console.error('获取支付记录失败:', error)
    currentOrderPayments.value = []
  }
  
  const depositRemaining = getRemainingDeposit(order, currentOrderPayments.value)
  const unpaid = getUnpaidRoomFee(order, currentOrderPayments.value)
  if (depositRemaining <= 0 && unpaid <= 0) {
    ElMessage.info('订单已付清，无需支付')
    return
  }
  
  // 根据已支付情况设置默认支付类型（选择未被禁用的选项）
  let defaultPaymentType = 'deposit'
  // 如果保证金已支付过或没有剩余保证金，则默认选择房费（如果房费还未支付）
  const canPayDeposit = !hasPaidDeposit(currentOrderPayments.value) && depositRemaining > 0
  const canPayRoomFee = !hasPaidRoomFee(currentOrderPayments.value) && unpaid > 0
  
  if (!canPayDeposit && canPayRoomFee) {
    defaultPaymentType = 'room_fee'
  } else if (canPayDeposit) {
    defaultPaymentType = 'deposit'
  } else if (canPayRoomFee) {
    defaultPaymentType = 'room_fee'
  }
  
  paymentForm.value = {
    paymentType: defaultPaymentType,
    paymentMethod: 'wechat',
    remark: ''
  }
  showPaymentDialog.value = true
}

const handlePaymentDialogClose = () => {
  showPaymentDialog.value = false
  currentOrder.value = null
  currentOrderPayments.value = []
}

const confirmPayment = async () => {
  if (!paymentForm.value.paymentMethod) {
    ElMessage.warning('请选择支付方式')
    return
  }

  if (!currentOrder.value) return

  // 防止重复支付：检查是否已经支付过该类型
  if (paymentForm.value.paymentType === 'deposit' && hasPaidDeposit(currentOrderPayments.value)) {
    ElMessage.warning('保证金已支付，不能重复支付')
    return
  }
  
  if (paymentForm.value.paymentType === 'room_fee' && hasPaidRoomFee(currentOrderPayments.value)) {
    ElMessage.warning('房费已支付，不能重复支付')
    return
  }

  const amount = getPaymentAmount()
  if (amount <= 0) {
    ElMessage.info('无需支付')
    return
  }

  paymentLoading.value = true
  try {
    await createPayment({
      orderId: currentOrder.value.id,
      amount,
      paymentType: paymentForm.value.paymentType,
      paymentMethod: paymentForm.value.paymentMethod,
      remark: paymentForm.value.remark
    })
    
    ElMessage.success('支付成功！')
    handlePaymentDialogClose()
    // 重新加载订单列表
    await loadOrders()
  } catch (error) {
    console.error('支付失败:', error)
    ElMessage.error(error.response?.data?.message || '支付失败，请重试')
  } finally {
    paymentLoading.value = false
  }
}

const handleCancel = async (id) => {
  try {
    await ElMessageBox.confirm('确定要取消这个订单吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await cancelOrder(id)
    ElMessage.success('订单已取消')
    loadOrders()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消订单失败:', error)
    }
  }
}

const handleLogout = () => {
  userStore.logout()
  ElMessage.success('已退出登录')
}

onMounted(() => {
  loadOrders()
})
</script>

<style scoped lang="scss">
.orders-page {
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

.orders-content {
  flex: 1;
  padding: 40px 0;
  background: #f5f7fa;

  .page-title {
    font-size: 28px;
    margin-bottom: 30px;
    color: #333;
  }

  .filter-tabs {
    margin-bottom: 30px;
  }

  .orders-list {
    .order-card {
      margin-bottom: 20px;

      .order-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 20px;
        padding-bottom: 15px;
        border-bottom: 1px solid #eee;

        .order-info {
          display: flex;
          align-items: center;
          gap: 15px;

          .order-no {
            font-size: 16px;
            font-weight: 600;
            color: #333;
          }
        }

        .order-actions {
          display: flex;
          gap: 10px;
        }
      }

      .order-body {
        display: grid;
        grid-template-columns: repeat(3, 1fr);
        gap: 15px;
        margin-bottom: 20px;

        @media (max-width: 768px) {
          grid-template-columns: repeat(2, 1fr);
        }

        .order-item {
          display: flex;

          .item-label {
            color: #999;
            margin-right: 5px;
          }

          .item-value {
            color: #333;
            font-weight: 500;
          }
        }
      }

      .order-footer {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding-top: 15px;
        border-top: 1px solid #eee;

        .price-info {
          display: flex;
          gap: 20px;

          .total-price {
            font-size: 18px;
            font-weight: 600;
            color: #667eea;
          }

          .deposit {
            font-size: 16px;
            color: #f56c6c;
          }

          .paid {
            font-size: 16px;
            color: #67c23a;
          }

          .unpaid {
            font-size: 16px;
            color: #e6a23c;
          }
        }

        .order-time {
          color: #999;
          font-size: 14px;
        }
      }
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



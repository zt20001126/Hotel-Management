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

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getMyOrders, cancelOrder } from '@/api/order'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)
const loading = ref(false)
const filterStatus = ref('')
const orders = ref([])
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

// 计算房费和额外消费的未结金额
const getUnpaidAmount = (order) => {
  if (!order) return 0
  const roomFee = Number(order.totalPrice || 0)
  const extraCharges = Number(order.extraCharges || 0)
  const paid = Number(order.paidAmount || 0)
  const totalDue = parseFloat((roomFee + extraCharges).toFixed(2))
  return Math.max(0, parseFloat((totalDue - paid).toFixed(2)))
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



const app = getApp()

Page({
  data: {
    order: null,
    payments: [],
    statusMap: { 
      pending: '待确认', 
      confirmed: '已确认', 
      checked_in: '已入住', 
      checked_out: '已退房', 
      cancelled: '已取消' 
    },
    statusHintMap: {
      pending: '房费已支付，等待管理员确认',
      confirmed: '订单已确认，期待您的到来',
      checked_in: '您已入住，祝您住得愉快',
      checked_out: '您已退房，期待再次相见',
      cancelled: '订单已取消'
    },
    paymentTypeMap: {
      deposit: '历史支付',
      room_fee: '房费（模拟支付）',
      balance: '补差价',
      refund: '退款'
    },
    unpaidAmount: 0,
    totalAmount: 0,
    nextPayAmount: 0,
    isModified: false,
    showPayModal: false,
    payAmount: 0,
    paying: false
  },

  onLoad(options) {
    this.loadOrder(options.id)
  },

  onShow() {
    if (this.data.order) {
      this.loadOrder(this.data.order.id)
    }
  },

  async loadOrder(id) {
    try {
      const res = await app.request({ url: `/order/${id}` })
      const order = res.data
      const roomFee = parseFloat(order.totalPrice || 0)
      const extraCharges = parseFloat(order.extraCharges || 0)
      const totalAmount = parseFloat((roomFee + extraCharges).toFixed(2))
      // 待付金额 = 应付总计 - 已付金额
      const paidAmount = parseFloat(order.paidAmount || 0)
      const unpaidAmount = parseFloat((totalAmount - paidAmount).toFixed(2))
      const nextPayAmount = unpaidAmount
      // 判断是否已改订过（通过检查 modifyCount）
      const isModified = order.modifyCount && order.modifyCount >= 1
      const money = value => Number(value || 0).toFixed(2)
      const displayMoney = {
        roomPrice: money(order.roomPrice), roomFee: money(roomFee),
        extraCharges: money(extraCharges),
        total: money(totalAmount), paid: money(paidAmount),
        unpaid: money(unpaidAmount), nextPay: money(nextPayAmount)
      }
      this.setData({ order, unpaidAmount, totalAmount, nextPayAmount, isModified, displayMoney })
      this.loadPayments(id)
    } catch (err) {
      console.error(err)
    }
  },

  async loadPayments(orderId) {
    try {
      const res = await app.request({ url: `/payment/order/${orderId}` })
      this.setData({ payments: (res.data || []).map(item => ({ ...item, displayAmount: Number(item.amount || 0).toFixed(2), displayTime: this.formatDate(item.createTime) })) })
    } catch (err) {
      console.error(err)
    }
  },

  async cancelOrder() {
    wx.showModal({
      title: '提示',
      content: '确定要取消此订单吗？',
      success: async (res) => {
        if (res.confirm) {
          try {
            await app.request({ url: `/order/${this.data.order.id}/cancel`, method: 'PUT' })
            wx.showToast({ title: '取消成功', icon: 'success' })
            this.loadOrder(this.data.order.id)
          } catch (err) {}
        }
      }
    })
  },

  formatDate(dateStr) {
    if (!dateStr) return ''
    const date = new Date(dateStr)
    const y = date.getFullYear()
    const m = String(date.getMonth() + 1).padStart(2, '0')
    const d = String(date.getDate()).padStart(2, '0')
    const h = String(date.getHours()).padStart(2, '0')
    const min = String(date.getMinutes()).padStart(2, '0')
    return `${y}-${m}-${d} ${h}:${min}`
  },

  stopPropagation() {
    // 阻止事件冒泡
  },

  goModify() {
    const { isModified } = this.data
    if (isModified) {
      wx.showToast({ title: '订单已改订过，不能重复改订', icon: 'none' })
      return
    }
    wx.navigateTo({ url: `/pages/order-modify/order-modify?orderId=${this.data.order.id}` })
  }
})

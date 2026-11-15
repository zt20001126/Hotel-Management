const app = getApp()

Page({
  data: {
    roomTypeId: '',
    checkInDate: '',
    checkOutDate: '',
    today: '',
    minCheckOut: '',
    roomType: {},
    roomImage: '',
    nights: 1,
    totalPrice: 0,
    guestOptions: [1, 2, 3, 4, 5, 6],
    guestIndex: 1,
    form: { guestName: '', guestPhone: '', guestIdCard: '', remark: '' },
    submitting: false
  },

  onLoad(options) {
    const today = this.formatDate(new Date())
    const tomorrowDate = new Date()
    tomorrowDate.setDate(tomorrowDate.getDate() + 1)
    const defaultCheckIn = options.checkInDate || today
    const defaultCheckOut = options.checkOutDate || this.formatDate(tomorrowDate)
    this.setData({
      roomTypeId: options.roomTypeId,
      today,
      minCheckOut: this.getMinCheckOut(defaultCheckIn),
      checkInDate: defaultCheckIn,
      checkOutDate: defaultCheckOut > defaultCheckIn ? defaultCheckOut : this.getMinCheckOut(defaultCheckIn),
      guestIndex: Math.max(0, (parseInt(options.guestCount) || 2) - 1)
    })
    this.loadRoomType()
  },

  async loadRoomType() {
    try {
      const res = await app.request({ url: `/roomType/${this.data.roomTypeId}` })
      const roomType = res.data
      let images = []
      try {
        images = typeof roomType.images === 'string' ? JSON.parse(roomType.images) : roomType.images
      } catch (e) {}
      this.setData({ roomType, roomImage: Array.isArray(images) ? (images[0] || '') : '' }, () => this.updatePrice())
    } catch (err) {
      console.error(err)
    }
  },

  onRoomImageError() {
    this.setData({ roomImage: '' })
  },

  calculateNights() {
    const { checkInDate, checkOutDate } = this.data
    const toDayNumber = value => {
      const [year, month, day] = value.split('-').map(Number)
      return Date.UTC(year, month - 1, day) / 86400000
    }
    return Math.max(1, toDayNumber(checkOutDate) - toDayNumber(checkInDate))
  },

  formatDate(date) {
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  },

  getMinCheckOut(checkInDate) {
    const date = new Date(`${checkInDate}T00:00:00`)
    date.setDate(date.getDate() + 1)
    return this.formatDate(date)
  },

  updatePrice() {
    if (!this.data.roomType.basePrice) return
    const nights = this.calculateNights()
    const totalPrice = (this.data.roomType.basePrice * nights).toFixed(2)
    this.setData({ nights, totalPrice })
  },

  onCheckInChange(e) {
    const checkInDate = e.detail.value
    const minCheckOut = this.getMinCheckOut(checkInDate)
    const checkOutDate = this.data.checkOutDate > checkInDate ? this.data.checkOutDate : minCheckOut
    this.setData({ checkInDate, minCheckOut, checkOutDate }, () => this.updatePrice())
  },

  onCheckOutChange(e) {
    const checkOutDate = e.detail.value
    if (checkOutDate <= this.data.checkInDate) {
      wx.showToast({ title: '退房日期必须晚于入住日期', icon: 'none' })
      return
    }
    this.setData({ checkOutDate }, () => this.updatePrice())
  },

  onInput(e) {
    const field = e.currentTarget.dataset.field
    this.setData({ [`form.${field}`]: e.detail.value })
  },

  onGuestChange(e) {
    this.setData({ guestIndex: e.detail.value })
  },

  async submitOrder() {
    const { form, guestOptions, guestIndex, roomTypeId, checkInDate, checkOutDate } = this.data
    if (!checkInDate || !checkOutDate || checkOutDate <= checkInDate) {
      wx.showToast({ title: '请选择有效的入住和退房日期', icon: 'none' })
      return
    }
    if (!form.guestName) { wx.showToast({ title: '请输入姓名', icon: 'none' }); return }
    if (!form.guestPhone) { wx.showToast({ title: '请输入手机号', icon: 'none' }); return }

    this.setData({ submitting: true })
    
    const orderData = {
      roomTypeId,
      checkInDate,
      checkOutDate,
      guestCount: guestOptions[guestIndex],
      guestName: form.guestName,
      guestPhone: form.guestPhone,
      guestIdCard: form.guestIdCard,
      remark: form.remark
    }
    
    // 记录订单提交日志
    console.log('[订单提交] 开始提交订单', {
      roomTypeId,
      checkInDate,
      checkOutDate,
      guestName: form.guestName,
      guestPhone: form.guestPhone,
      guestCount: guestOptions[guestIndex],
      timestamp: new Date().toISOString()
    })
    
    try {
      const res = await app.request({
        url: '/order',
        method: 'POST',
        data: orderData
      })
      
      // 记录成功日志
      console.log('[订单提交] 订单创建成功', {
        orderId: res.data.id,
        orderNo: res.data.orderNo,
        totalPrice: res.data.totalPrice,
        timestamp: new Date().toISOString()
      })
      
      wx.showToast({ title: '模拟支付成功', icon: 'success' })
      setTimeout(() => {
        wx.redirectTo({ url: `/pages/order-detail/order-detail?id=${res.data.id}` })
      }, 1500)
    } catch (err) {
      // 详细记录错误信息
      const errorInfo = {
        error: err.message || '未知错误',
        code: err.code || err.statusCode || 'N/A',
        data: err.data || err,
        orderData: orderData,
        timestamp: new Date().toISOString()
      }
      console.error('[订单提交] 订单创建失败', errorInfo)
      
      // 显示用户友好的错误提示
      const errorMessage = err.message || (err.data && err.data.message) || '订单创建失败，请稍后重试'
      wx.showToast({ 
        title: errorMessage, 
        icon: 'none',
        duration: 3000
      })
      
      // 将错误信息保存到本地存储，方便后续排查
      try {
        const errorLogs = wx.getStorageSync('order_error_logs') || []
        errorLogs.unshift({
          ...errorInfo,
          id: Date.now()
        })
        // 只保留最近50条错误日志
        if (errorLogs.length > 50) {
          errorLogs.splice(50)
        }
        wx.setStorageSync('order_error_logs', errorLogs)
      } catch (storageErr) {
        console.error('[订单提交] 保存错误日志失败', storageErr)
      }
    } finally {
      this.setData({ submitting: false })
    }
  }
})

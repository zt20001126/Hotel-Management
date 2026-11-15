const app = getApp()

Page({
  data: {
    today: '',
    checkInDate: '',
    checkOutDate: '',
    minCheckOut: '',
    stayNights: 1,
    guestOptions: [1, 2, 3, 4, 5, 6],
    guestIndex: 1
  },

  onLoad() {
    const today = this.formatDate(new Date())
    const tomorrow = this.formatDate(new Date(Date.now() + 86400000))
    this.setData({
      today,
      checkInDate: today,
      checkOutDate: tomorrow,
      minCheckOut: tomorrow,
      stayNights: this.calculateNights(today, tomorrow)
    })
  },

  onShow() {
    const tabBar = this.getTabBar && this.getTabBar()
    if (tabBar) tabBar.setData({ selected: 0 })
  },

  formatDate(date) {
    const y = date.getFullYear()
    const m = String(date.getMonth() + 1).padStart(2, '0')
    const d = String(date.getDate()).padStart(2, '0')
    return `${y}-${m}-${d}`
  },

  // Parse calendar dates as UTC day numbers to avoid DST and local timezone offsets.
  calculateNights(checkInDate, checkOutDate) {
    if (!checkInDate || !checkOutDate) return 0
    const toDayNumber = (value) => {
      const parts = value.split('-').map(Number)
      return Date.UTC(parts[0], parts[1] - 1, parts[2]) / 86400000
    }
    return Math.max(0, toDayNumber(checkOutDate) - toDayNumber(checkInDate))
  },

  onCheckInChange(e) {
    const checkInDate = e.detail.value
    const minCheckOut = this.formatDate(new Date(new Date(checkInDate).getTime() + 86400000))
    let checkOutDate = this.data.checkOutDate
    if (checkOutDate <= checkInDate) {
      checkOutDate = minCheckOut
    }
    this.setData({ checkInDate, minCheckOut, checkOutDate, stayNights: this.calculateNights(checkInDate, checkOutDate) })
  },

  onCheckOutChange(e) {
    const checkOutDate = e.detail.value
    if (checkOutDate <= this.data.checkInDate) {
      wx.showToast({ title: '退房日期必须晚于入住日期', icon: 'none' })
      return
    }
    this.setData({ checkOutDate, stayNights: this.calculateNights(this.data.checkInDate, checkOutDate) })
  },

  onGuestChange(e) {
    this.setData({ guestIndex: e.detail.value })
  },

  searchRooms() {
    const { checkInDate, checkOutDate, guestIndex, guestOptions } = this.data
    
    // 验证日期
    if (!checkInDate || !checkOutDate) {
      wx.showToast({ title: '请选择入住和退房日期', icon: 'none' })
      return
    }
    
    if (new Date(checkInDate) >= new Date(checkOutDate)) {
      wx.showToast({ title: '退房日期必须晚于入住日期', icon: 'none' })
      return
    }
    
    const guestCount = guestOptions[guestIndex]
    // 通过全局变量传递搜索参数（因为rooms是tabBar页面，不能用navigateTo传参）
    app.globalData.searchParams = {
      checkInDate,
      checkOutDate,
      guestCount
    }
    wx.switchTab({
      url: '/pages/rooms/rooms'
    })
  }
})

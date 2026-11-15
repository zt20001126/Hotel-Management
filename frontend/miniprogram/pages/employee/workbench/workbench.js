const app = getApp()

Page({
  data: {
    userInfo: {},
    role: '',
    statistics: {
      todayCheckIn: 0,
      todayCheckOut: 0,
      pendingCleaning: 0,
      myTasks: 0
    },
    quickActions: []
  },

  onLoad() {
    this.checkEmployeeLogin()
  },

  onShow() {
    this.loadUserInfo()
    this.loadStatistics()
  },

  checkEmployeeLogin() {
    const userInfo = app.globalData.userInfo
    if (!app.checkLogin() || !userInfo || !userInfo.role) {
      wx.showToast({ title: '请先登录', icon: 'none' })
      setTimeout(() => {
        wx.redirectTo({ url: '/pages/login/login' })
      }, 1500)
      return false
    }
    
    // 检查是否是员工
    if (userInfo.role === 'guest') {
      wx.showToast({ title: '请使用员工账号登录', icon: 'none' })
      setTimeout(() => {
        wx.redirectTo({ url: '/pages/login/login' })
      }, 1500)
      return false
    }

    this.setData({ 
      userInfo: userInfo,
      role: userInfo.role || ''
    })
    this.initQuickActions()
    return true
  },

  initQuickActions() {
    const role = this.data.role
    let actions = []

    // 经营者/管理员：所有功能（包含订单管理）
    if (role === '经营者' || role === '管理员' || role === 'manager' || role === 'owner' || role === 'admin') {
      actions = [
        { icon: '📋', title: '今日入住', path: '/pages/employee/orders/orders?type=checkin', color: '#667eea' },
        { icon: '🚪', title: '今日退房', path: '/pages/employee/orders/orders?type=checkout', color: '#48bb78' },
        { icon: '📝', title: '订单管理', path: '/pages/employee/orders/orders', color: '#4299e1' },
        { icon: '🏠', title: '房间管理', path: '/pages/employee/rooms/rooms', color: '#ed8936' },
        { icon: '🧹', title: '清洁任务', path: '/pages/employee/cleaning/cleaning', color: '#38b2ac' },
        { icon: '💳', title: 'POS消费', path: '/pages/employee/pos/pos', color: '#e53e3e' },
        { icon: '📊', title: '数据统计', path: '/pages/employee/statistics/statistics', color: '#9f7aea' },
        { icon: '📦', title: '库存管理', path: '/pages/employee/inventory/inventory', color: '#f56565' }
      ]
    }
    // 前台接待：订单相关
    else if (role === '前台接待' || role === 'receptionist' || role === 'frontdesk') {
      actions = [
        { icon: '📋', title: '今日入住', path: '/pages/employee/orders/orders?type=checkin', color: '#667eea' },
        { icon: '🚪', title: '今日退房', path: '/pages/employee/orders/orders?type=checkout', color: '#48bb78' },
        { icon: '📝', title: '订单管理', path: '/pages/employee/orders/orders', color: '#ed8936' },
        { icon: '💳', title: 'POS消费', path: '/pages/employee/pos/pos', color: '#e53e3e' },
        { icon: '🏠', title: '房间状态', path: '/pages/employee/rooms/rooms', color: '#38b2ac' }
      ]
    }
    // 房务人员：仅清洁和房间状态，无订单管理
    else if (role === '房务人员' || role === 'housekeeping' || role === 'cleaner' || role === 'housekeeper') {
      actions = [
        { icon: '🧹', title: '我的任务', path: '/pages/employee/cleaning/cleaning?type=my', color: '#667eea' },
        { icon: '📋', title: '待分配', path: '/pages/employee/cleaning/cleaning?type=pending', color: '#48bb78' },
        { icon: '🏠', title: '房间状态', path: '/pages/employee/rooms/rooms', color: '#ed8936' }
      ]
    }
    // 默认功能（无订单管理）
    else {
      actions = [
        { icon: '🏠', title: '房间管理', path: '/pages/employee/rooms/rooms', color: '#667eea' },
        { icon: '🧹', title: '清洁任务', path: '/pages/employee/cleaning/cleaning', color: '#48bb78' }
      ]
    }

    this.setData({ quickActions: actions })
  },

  async loadUserInfo() {
    try {
      const res = await app.request({ url: '/employee/current' })
      if (res.data) {
        app.login(app.globalData.token, res.data)
        this.setData({ 
          userInfo: res.data,
          role: res.data.role || ''
        })
        this.initQuickActions()
      }
    } catch (err) {
      console.error('加载用户信息失败', err)
    }
  },

  async loadStatistics() {
    try {
      // 今日入住
      const checkInRes = await app.request({ url: '/order/today-checkin' })
      // 今日退房
      const checkOutRes = await app.request({ url: '/order/today-checkout' })
      // 待分配清洁任务
      const pendingRes = await app.request({ url: '/cleaning/pending' })
      // 我的任务
      const myTasksRes = await app.request({ url: '/cleaning/my' })

      this.setData({
        statistics: {
          todayCheckIn: (checkInRes.data && checkInRes.data.length) || 0,
          todayCheckOut: (checkOutRes.data && checkOutRes.data.length) || 0,
          pendingCleaning: (pendingRes.data && pendingRes.data.length) || 0,
          myTasks: (myTasksRes.data && myTasksRes.data.length) || 0
        }
      })
    } catch (err) {
      console.error('加载统计数据失败', err)
    }
  },

  navigateTo(e) {
    const path = e.currentTarget.dataset.path
    if (path) {
      wx.navigateTo({ url: path })
    }
  },

  logout() {
    wx.showModal({
      title: '提示',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          app.logout()
          app.goToLogin()
        }
      }
    })
  }
})


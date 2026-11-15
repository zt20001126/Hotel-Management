const app = getApp()

Page({
  data: {
    isLogin: false,
    userInfo: {},
    avatarUrl: '',
    isEmployee: false // 是否是员工
  },

  onShow() {
    const tabBar = this.getTabBar && this.getTabBar()
    if (tabBar) tabBar.setData({ selected: 3 })
    const userInfo = app.globalData.userInfo || {}
    const isEmployee = userInfo.role && userInfo.role !== 'guest'
    this.setData({
      isLogin: app.checkLogin(),
      userInfo: userInfo,
      avatarUrl: userInfo.avatar || '/images/avatar-ink.png',
      isEmployee: isEmployee
    })
    // 如果已登录，刷新用户信息
    if (app.checkLogin()) {
      if (isEmployee) {
        this.loadEmployeeInfo()
      } else {
        this.loadUserInfo()
      }
    }
  },

  onAvatarError() {
    // 图片地址无效时回退为本地国风默认头像。
    this.setData({
      avatarUrl: '/images/avatar-ink.png'
    })
  },

  async loadUserInfo() {
    try {
      const res = await app.request({ url: '/guest/current' })
      const userData = res.data || {}
      app.login(app.globalData.token, userData)
      this.setData({ 
        userInfo: userData,
        avatarUrl: userData.avatar || '/images/avatar-ink.png',
        isEmployee: false
      })
    } catch (err) {
      console.error(err)
    }
  },

  async loadEmployeeInfo() {
    try {
      const res = await app.request({ url: '/employee/current' })
      const userData = res.data || {}
      app.login(app.globalData.token, userData)
      this.setData({ 
        userInfo: userData,
        avatarUrl: userData.avatar || '/images/avatar-ink.png',
        isEmployee: true
      })
    } catch (err) {
      console.error(err)
    }
  },

  goLogin() {
    wx.navigateTo({ url: '/pages/login/login' })
  },

  goOrders() {
    wx.switchTab({ url: '/pages/orders/orders' })
  },

  goEditProfile() {
    wx.navigateTo({ url: '/pages/profile-edit/profile-edit' })
  },

  logout() {
    wx.showModal({
      title: '提示',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          app.logout()
          this.setData({ isLogin: false, userInfo: {} })
          app.goToLogin()
        }
      }
    })
  },

  goContact() {
    wx.navigateTo({ url: '/pages/contact/contact' })
  },

  goWorkbench() {
    wx.navigateTo({ url: '/pages/employee/workbench/workbench' })
  }
})

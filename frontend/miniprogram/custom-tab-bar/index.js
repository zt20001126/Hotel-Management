Component({
  data: {
    selected: 0,
    list: [
      { pagePath: '/pages/index/index', text: '首页', icon: '/images/tab-home.png', activeIcon: '/images/tab-home-active.png' },
      { pagePath: '/pages/rooms/rooms', text: '房型', icon: '/images/tab-room.png', activeIcon: '/images/tab-room-active.png' },
      { pagePath: '/pages/orders/orders', text: '订单', icon: '/images/tab-order.png', activeIcon: '/images/tab-order-active.png' },
      { pagePath: '/pages/profile/profile', text: '我的', icon: '/images/tab-user.png', activeIcon: '/images/tab-user-active.png' }
    ]
  },
  pageLifetimes: {
    show() {
      const pages = getCurrentPages()
      const route = pages[pages.length - 1].route
      const selected = this.data.list.findIndex(item => item.pagePath === `/${route}`)
      if (selected >= 0) this.setData({ selected })
    }
  },
  methods: {
    switchTab(e) {
      const path = e.currentTarget.dataset.path
      if (!path) return
      wx.switchTab({
        url: path,
        fail(err) {
          console.error('TabBar 页面切换失败:', path, err)
        }
      })
    }
  }
})

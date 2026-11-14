const app = getApp()

Page({
  data: {
    checkInDate: '',
    checkOutDate: '',
    guestCount: 2,
    roomTypes: [],
    loading: false,
    isEmpty: false,
    today: '',
    minCheckOut: ''
  },

  onLoad(options) {
    this._searchRequestId = 0
    // 设置今天的日期
    const today = new Date()
    const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`
    
    this.setData({
      today: todayStr,
      minCheckOut: todayStr
    })
  },
  
  onShow() {
    const tabBar = this.getTabBar && this.getTabBar()
    if (tabBar) tabBar.setData({ selected: 1 })
    // 检查是否有从首页传递的搜索参数
    const searchParams = app.globalData.searchParams
    if (searchParams) {
      const guestCount = parseInt(searchParams.guestCount) || 2
      this.setData({
        checkInDate: searchParams.checkInDate || '',
        checkOutDate: searchParams.checkOutDate || '',
        guestCount: guestCount,
        minCheckOut: this.getMinCheckOut(searchParams.checkInDate)
      })
      
      // 清除搜索参数，避免重复触发
      app.globalData.searchParams = null
      
      // 自动加载房型
      if (searchParams.checkInDate && searchParams.checkOutDate) {
        this.loadRoomTypes()
      }
    }
  },
  
  // 计算最小退房日期
  getMinCheckOut(checkInDate) {
    if (!checkInDate) {
      return this.data.today
    }
    // 退房日期至少是入住日期的下一天
    const checkIn = new Date(checkInDate)
    checkIn.setDate(checkIn.getDate() + 1)
    const year = checkIn.getFullYear()
    const month = String(checkIn.getMonth() + 1).padStart(2, '0')
    const day = String(checkIn.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  },
  
  onCheckInChange(e) {
    const checkInDate = e.detail.value
    const minCheckOut = this.getMinCheckOut(checkInDate)
    const checkOutDate = this.data.checkOutDate >= minCheckOut ? this.data.checkOutDate : minCheckOut
    this.setData({ checkInDate, checkOutDate, minCheckOut }, () => this.loadRoomTypes())
  },
  
  onCheckOutChange(e) {
    const checkOutDate = e.detail.value
    if (!this.data.checkInDate || checkOutDate <= this.data.checkInDate) {
      wx.showToast({ title: '退房日期必须晚于入住日期', icon: 'none' })
      return
    }
    this.setData({ checkOutDate }, () => this.loadRoomTypes())
  },

  async loadRoomTypes() {
    const { checkInDate, checkOutDate, guestCount } = this.data
    
    // 验证日期
    if (!checkInDate || !checkOutDate) {
      return
    }
    
    if (new Date(checkInDate) >= new Date(checkOutDate)) {
      wx.showToast({ title: '退房日期必须晚于入住日期', icon: 'none' })
      return
    }
    
    const requestId = ++this._searchRequestId
    this.setData({ loading: true, isEmpty: false })
    
    try {
      const res = await app.request({
        url: '/roomType/search',
        method: 'GET',
        data: { 
          checkInDate, 
          checkOutDate, 
          guestCount: parseInt(guestCount) 
        }
      })
      
      // 解析图片列表
      const roomTypes = (res.data || []).map(roomType => {
        if (roomType.images) {
          try {
            roomType.images = typeof roomType.images === 'string' 
              ? JSON.parse(roomType.images) 
              : roomType.images
          } catch (e) {
            roomType.images = []
          }
        } else {
          roomType.images = []
        }
        // 如果没有图片，使用网络占位图
        if (!roomType.images || roomType.images.length === 0) {
          roomType.images = ['https://dummyimage.com/400x300/667eea/ffffff.png&text=Room']
        }
        return roomType
      })
      
      if (requestId !== this._searchRequestId) return
      
      this.setData({ 
        roomTypes,
        isEmpty: roomTypes.length === 0,
        loading: false
      })
      
      if (roomTypes.length === 0) {
        wx.showToast({ title: '暂无可用房型', icon: 'none' })
      }
    } catch (err) {
      if (requestId !== this._searchRequestId) return
      console.error('搜索房型失败:', err)
      this.setData({ loading: false, isEmpty: true })
      wx.showToast({ 
        title: err.message || '搜索失败，请稍后重试', 
        icon: 'none',
        duration: 2000
      })
    }
  },

  onImageError(e) {
    const index = e.currentTarget.dataset.index
    const roomTypes = this.data.roomTypes
    if (roomTypes[index]) {
      roomTypes[index].images[0] = 'https://dummyimage.com/400x300/667eea/ffffff.png&text=Room'
      this.setData({ roomTypes })
    }
  },

  goDetail(e) {
    const id = e.currentTarget.dataset.id
    const { checkInDate, checkOutDate, guestCount } = this.data
    wx.navigateTo({
      url: `/pages/room-detail/room-detail?id=${id}&checkInDate=${checkInDate}&checkOutDate=${checkOutDate}&guestCount=${guestCount}`
    })
  }
})

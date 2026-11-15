const app = getApp()

Page({
  data: {
    id: '',
    checkInDate: '',
    checkOutDate: '',
    guestCount: 2,
    roomType: null,
    facilities: [],
    nights: 1,
    totalPrice: 0,
    nightlyPrice: '0.00',
    currentImage: 0
  },

  onLoad(options) {
    this.setData({
      id: options.id,
      checkInDate: options.checkInDate || '',
      checkOutDate: options.checkOutDate || '',
      guestCount: options.guestCount || 2
    })
    this.loadRoomType()
  },

  async loadRoomType() {
    try {
      const res = await app.request({ url: `/roomType/${this.data.id}` })
      const roomType = res.data
      let facilities = []
      if (roomType.facilities) {
        try { facilities = typeof roomType.facilities === 'string' ? JSON.parse(roomType.facilities) : roomType.facilities } catch (e) {}
      }
      
      // 解析图片列表
      let images = []
      if (roomType.images) {
        try {
          images = typeof roomType.images === 'string' 
            ? JSON.parse(roomType.images) 
            : roomType.images
        } catch (e) {
          images = []
        }
      }
      if (!Array.isArray(images)) images = []
      roomType.images = images
      
      const nights = this.calculateNights()
      const totalPrice = (roomType.basePrice * nights).toFixed(2)
      
      facilities = Array.isArray(facilities) ? facilities.map(name => ({ name, icon: this.facilityIcon(name) })) : []
      this.setData({ roomType, facilities, nights, totalPrice, nightlyPrice: Number(roomType.basePrice || 0).toFixed(2), currentImage: 0 })
    } catch (err) {
      console.error(err)
    }
  },

  facilityIcon(name) {
    const icons = [
      [/智能|马桶/, 'smart'], [/地暖|暖气/, 'heat'], [/空调/, 'air'],
      [/电视/, 'tv'], [/Wi.?Fi|无线|宽带/i, 'wifi'], [/茶椅|休闲椅|沙发/, 'chair'],
      [/茶几|茶桌|桌/, 'table'], [/衣柜|衣帽/, 'cabinet'], [/卫浴|洗浴|浴缸|淋浴/, 'bath'],
      [/窗/, 'window'], [/禁烟/, 'no-smoking'], [/加床|床/, 'bed'], [/吹风机/, 'dryer']
    ]
    const match = icons.find(([pattern]) => pattern.test(name))
    return `./icons/${match ? match[1] : 'facility'}.svg`
  },

  onSwiperChange(e) {
    this.setData({ currentImage: e.detail.current })
  },

  calculateNights() {
    const { checkInDate, checkOutDate } = this.data
    if (!checkInDate || !checkOutDate) return 1
    const start = new Date(checkInDate)
    const end = new Date(checkOutDate)
    return Math.max(1, Math.ceil((end - start) / 86400000))
  },

  onImageError(e) {
    const index = e.currentTarget.dataset.index
    const roomType = this.data.roomType
    if (roomType && roomType.images && roomType.images[index]) {
      roomType.images.splice(index, 1)
      this.setData({ roomType, currentImage: Math.max(0, Math.min(this.data.currentImage, roomType.images.length - 1)) })
    }
  },

  goBooking() {
    if (!app.checkLogin()) {
      wx.navigateTo({ url: '/pages/login/login' })
      return
    }
    const { id, checkInDate, checkOutDate, guestCount } = this.data
    wx.navigateTo({
      url: `/pages/booking/booking?roomTypeId=${id}&checkInDate=${checkInDate}&checkOutDate=${checkOutDate}&guestCount=${guestCount}`
    })
  }
})

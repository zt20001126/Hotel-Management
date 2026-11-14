package com.homestay.module.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.homestay.module.system.entity.Guest;

public interface GuestService extends IService<Guest> {
    
    Guest loginByWechatPhone(String openid, String phone);
    
    IPage<Guest> getGuestPage(Integer page, Integer size, String keyword);
    
    boolean updateGuest(Guest guest);
    
}

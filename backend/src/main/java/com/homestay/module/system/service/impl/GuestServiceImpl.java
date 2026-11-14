package com.homestay.module.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.homestay.common.exception.BusinessException;
import com.homestay.module.system.entity.Guest;
import com.homestay.module.system.mapper.GuestMapper;
import com.homestay.module.system.service.GuestService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GuestServiceImpl extends ServiceImpl<GuestMapper, Guest> implements GuestService {

    @Override
    @Transactional
    public Guest loginByWechatPhone(String openid, String phone) {
        if (!StringUtils.hasText(openid) || !StringUtils.hasText(phone)) {
            throw new BusinessException("微信身份或手机号无效");
        }

        Guest guestByOpenid = this.getOne(new LambdaQueryWrapper<Guest>()
                .eq(Guest::getOpenid, openid));
        Guest guestByPhone = this.getOne(new LambdaQueryWrapper<Guest>()
                .eq(Guest::getPhone, phone));
        Guest guest;

        if (guestByOpenid != null && guestByPhone != null
                && !guestByOpenid.getId().equals(guestByPhone.getId())) {
            throw new BusinessException("该手机号已关联其他宾客账号，请联系客服");
        }

        if (guestByOpenid != null) {
            guest = guestByOpenid;
            if (guest.getStatus() == null || guest.getStatus() != 1) {
                throw new BusinessException("账号已停用，请联系客服");
            }
            guest.setPhone(phone);
            applyDefaultProfile(guest, phone);
            this.updateById(guest);
            return guest;
        }

        if (guestByPhone != null) {
            if (guestByPhone.getOpenid() != null) {
                throw new BusinessException("该手机号已关联其他微信账号，请联系客服");
            }
            if (guestByPhone.getStatus() == null || guestByPhone.getStatus() != 1) {
                throw new BusinessException("账号已停用，请联系客服");
            }
            guest = guestByPhone;
            guest.setOpenid(openid);
            applyDefaultProfile(guest, phone);
            this.updateById(guest);
            return guest;
        }

        guest = new Guest();
        guest.setOpenid(openid);
        guest.setPhone(phone);
        guest.setStatus(1);
        guest.setGender(0);
        applyDefaultProfile(guest, phone);
        this.save(guest);
        return guest;
    }

    private void applyDefaultProfile(Guest guest, String phone) {
        if (!StringUtils.hasText(guest.getNickname())) {
            guest.setNickname("微信用户" + phone.substring(Math.max(0, phone.length() - 4)));
        }
        if (!StringUtils.hasText(guest.getAvatar())) {
            guest.setAvatar("/images/logo.png");
        }
    }

    @Override
    public IPage<Guest> getGuestPage(Integer page, Integer size, String keyword) {
        Page<Guest> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Guest> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Guest::getUsername, keyword)
                    .or().like(Guest::getNickname, keyword)
                    .or().like(Guest::getRealName, keyword)
                    .or().like(Guest::getPhone, keyword));
        }
        
        wrapper.orderByDesc(Guest::getCreateTime);
        return this.page(pageParam, wrapper);
    }

    @Override
    public boolean updateGuest(Guest guest) {
        Guest existing = this.getById(guest.getId());
        if (existing == null) {
            throw new BusinessException("用户不存在");
        }
        guest.setUsername(null);
        guest.setPassword(null);
        guest.setOpenid(null);
        guest.setPhone(null);
        guest.setStatus(null);
        guest.setDeleted(null);
        return this.updateById(guest);
    }
}

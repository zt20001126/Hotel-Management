package com.homestay.module.system.controller;

import com.homestay.common.result.Result;
import com.homestay.common.util.JwtUtil;
import com.homestay.module.system.entity.Employee;
import com.homestay.module.system.entity.Guest;
import com.homestay.module.system.integration.WechatMiniProgramClient;
import com.homestay.module.system.service.EmployeeService;
import com.homestay.module.system.service.GuestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "认证管理")
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private EmployeeService employeeService;
    
    @Autowired
    private GuestService guestService;
    
    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private WechatMiniProgramClient wechatMiniProgramClient;

    @Operation(summary = "员工登录")
    @PostMapping("/employee/login")
    public Result<Map<String, Object>> employeeLogin(@RequestBody LoginRequest request) {
        Employee employee = employeeService.login(request.getUsername(), request.getPassword());
        
        String token = jwtUtil.generateToken(employee.getId(), employee.getUsername(), 
                "employee", employee.getRole());
        
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userInfo", employee);
        employee.setPassword(null);
        
        return Result.success(data);
    }

    @Operation(summary = "微信手机号登录")
    @PostMapping("/wechat/phone-login")
    public Result<Map<String, Object>> wechatPhoneLogin(@RequestBody WechatPhoneLoginRequest request) {
        String openid = wechatMiniProgramClient.getOpenid(request.getLoginCode());
        String phone = wechatMiniProgramClient.getPhoneNumber(request.getPhoneCode());
        Guest guest = guestService.loginByWechatPhone(openid, phone);

        String token = jwtUtil.generateToken(guest.getId(), guest.getUsername(), "guest", "guest");
        guest.setPassword(null);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userInfo", guest);
        return Result.success(data);
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }

    @Data
    public static class WechatPhoneLoginRequest {
        private String loginCode;
        private String phoneCode;
    }
}

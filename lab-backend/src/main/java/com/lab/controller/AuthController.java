package com.lab.controller;

import com.lab.common.Result;
import com.lab.dto.LoginDTO;
import com.lab.dto.RegisterDTO;
import com.lab.entity.SysUser;
import com.lab.service.EmailService;
import com.lab.service.SysUserService;
import com.lab.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final SysUserService sysUserService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    public AuthController(SysUserService sysUserService, 
                         PasswordEncoder passwordEncoder, 
                         JwtUtil jwtUtil,
                         EmailService emailService) {
        this.sysUserService = sysUserService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Validated @RequestBody LoginDTO dto) {
        // 从sys_user表中查找
        SysUser user = sysUserService.getByUsername(dto.getUsername());
        
        if (user == null) return Result.fail("账号不存在");
        
        // 检查状态
        Integer status = user.getStatus();
        String password = user.getPassword();
        Long userId = user.getId();
        String realName = user.getRealName();
        String avatar = user.getAvatar();
        String role = user.getRole();
        
        if (status != null && status == 0) return Result.fail("账号已禁用");
        // 明文密码比对（不使用加密）
        if (!dto.getPassword().equals(password)) return Result.fail("密码错误");
        
        String token = jwtUtil.generateToken(userId, realName, role);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", userId);
        data.put("username", dto.getUsername());
        data.put("realName", realName);
        data.put("role", role);
        data.put("avatar", avatar);
        return Result.ok(data);
    }

    @PostMapping("/register")
    public Result<Void> register(@Validated @RequestBody RegisterDTO dto) {
        if (!"TEACHER".equals(dto.getRole()) && !"STUDENT".equals(dto.getRole())) {
            return Result.fail("仅支持教师或学生注册");
        }
        
        // 检查用户名是否已存在
        if (sysUserService.getByUsername(dto.getUsername()) != null) {
            return Result.fail("账号已存在");
        }
        
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(dto.getPassword()); // 明文密码
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setRole(dto.getRole());
        user.setStatus(1);
        sysUserService.createUser(user);
        
        return Result.ok();
    }

    @GetMapping("/info")
    public Result<Object> getInfo(@RequestAttribute("userId") Long userId, @RequestAttribute("userRole") String role) {
        SysUser user = sysUserService.getById(userId);
        
        if (user == null) return Result.fail("用户不存在");
        
        // 清除密码
        user.setPassword(null);
        
        return Result.ok(user);
    }
    
    /**
     * 发送重置密码验证码
     */
    @PostMapping("/send-reset-code")
    public Result<Void> sendResetCode(@RequestParam String email) {
        // 检查邮箱是否存在
        SysUser user = sysUserService.getByEmail(email);
        if (user == null) {
            return Result.fail("该邮箱未注册");
        }
        
        // 检查是否可以发送（60 秒内只能发送一次）
        if (!emailService.canSendCode(email)) {
            return Result.fail("发送过于频繁，请 60 秒后再试");
        }
        
        // 生成并发送验证码
        emailService.generateAndSendCode(email);
        
        return Result.ok();
    }
    
    /**
     * 验证验证码并重置密码
     */
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(
            @RequestParam String email,
            @RequestParam String code,
            @RequestParam String newPassword) {
        
        // 验证邮箱是否存在
        SysUser user = sysUserService.getByEmail(email);
        if (user == null) {
            return Result.fail("该邮箱未注册");
        }
        
        // 验证验证码
        if (!emailService.verifyCode(email, code)) {
            return Result.fail("验证码错误或已过期");
        }
        
        // 重置密码
        user.setPassword(newPassword);
        sysUserService.updateById(user);
        
        return Result.ok();
    }
}

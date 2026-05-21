package com.weaving.llm.app.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.weaving.llm.common.domain.R;
import com.weaving.llm.common.domain.User;
import com.weaving.llm.common.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Author: 渚濇ⅵ
 * @Date: 2025/10/27
 * @Description: 用户管理控制器
 */
@Slf4j
@Tag(name = "用户管理", description = "用户增删改查管理接口")
@RestController
@RequestMapping("/v0/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    @Operation(summary = "获取所有用户", description = "查询系统中所有用户列表")
    public R<List<User>> getAllUsers() {
        return R.ok(userService.list());
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询用户", description = "分页查询用户列表")
    public R<Page<User>> getUsersByPage(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页数量", example = "10") @RequestParam(defaultValue = "10") Integer size) {
        return R.ok(userService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取用户详情", description = "根据用户 ID 查询详细信息")
    public R<User> getUserById(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            return R.fail("用户不存在");
        }
        return R.ok(user);
    }

    @PostMapping
    @Operation(summary = "创建用户", description = "创建新用户，默认密码 123456")
    public R<Boolean> createUser(@RequestBody User user) {
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            user.setPassword(org.springframework.util.DigestUtils.md5DigestAsHex("123456".getBytes()));
        }
        if (user.getStatus() == null || user.getStatus().isEmpty()) {
            user.setStatus("pending");
        }
        return R.ok(userService.save(user));
    }

    @PutMapping
    @Operation(summary = "更新用户", description = "更新现有用户信息")
    public R<Boolean> updateUser(@RequestBody User user) {
        return R.ok(userService.updateById(user));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户", description = "根据用户 ID 删除用户")
    public R<Boolean> deleteUser(@PathVariable Long id) {
        return R.ok(userService.removeById(id));
    }
}

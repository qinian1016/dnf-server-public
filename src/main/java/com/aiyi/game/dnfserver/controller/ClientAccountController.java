package com.aiyi.game.dnfserver.controller;

import com.aiyi.game.dnfserver.conf.NoLogin;
import com.aiyi.game.dnfserver.entity.AccountVO;
import com.aiyi.game.dnfserver.service.AccountService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 客户端账号操作Web接口
 * @author gsk
 */
@RestController
public class ClientAccountController {

    @Resource
    private AccountService accountService;

    /**
     * 客户端用户登录接口
     * @param accountVO
     *          要登录的用户
     * @return String
     *          该用户的客户端授权Key
     */
    @RequestMapping("client/login")
    @NoLogin
    public String loginClient(AccountVO accountVO){
        return accountService.loginClient(accountVO, false);
    }

    @PostMapping("api/v1/client/login")
    @NoLogin
    public Map<String, String> loginClieng2(@RequestBody AccountVO accountVO){
        Map<String, String> map = new HashMap<>();
        map.put("token", accountService.loginClient(accountVO, true));
        return map;
    }

    /**
     * 注册
     * @param accountVO
     * @return
     */
    @GetMapping("client/register")
    @NoLogin
    public String register(AccountVO accountVO){
        accountService.register(accountVO);
        return "注册成功";
    }

    /**
     * 注册
     * @param accountVO
     * @return
     */
    @PostMapping("api/v1/client/register")
    @NoLogin
    public void register2(@RequestBody AccountVO accountVO){
        accountService.register(accountVO);
    }
}

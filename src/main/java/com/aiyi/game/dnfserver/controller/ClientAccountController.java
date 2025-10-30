package com.aiyi.game.dnfserver.controller;

import com.aiyi.core.beans.ResultBean;
import com.aiyi.game.dnfserver.conf.NoLogin;
import com.aiyi.game.dnfserver.entity.AccountVO;
import com.aiyi.game.dnfserver.service.AccountService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    public Map<String, Object> register2(@RequestBody AccountVO accountVO){
        accountService.register(accountVO);
        Map<String, Object> map = new HashMap<>();
        map.put("message", "注册成功");
        return map;
    }


    /**
     * 获取完整客户端安装包信息
     * @return
     */
    @GetMapping("api/v1/client/full-package")
    @NoLogin
    public Map<String, Object> getFullPackage(){
        Map<String, Object> map = new HashMap<>();
        map.put("version", "1.0.0");
        map.put("downloadUrl", "https://oss.icoding.ink/.inner/dnf/install.zip");
        return map;
    }

    /**
     * 获取客户端更新信息
     * @return
     */
    @GetMapping("api/v1/client/version")
    @NoLogin
    public Map<String, Object> getClientUpdateInfo(){
        Map<String, Object> map = new HashMap<>();
        map.put("version", "1.0.0");
        map.put("downloadUrl", "https://oss.icoding.ink/.inner/dnf/update/client_update_1.0.0.zip");
        map.put("description", "增加【新世界的选择】任务，新角色可在赛利亚处领取该任务，完成后可获得丰厚奖励。");
        return map;
    }

    /**
     * 获取客户端大图列表
     * @return
     */
    @GetMapping("api/v1/client/big-pic-list")
    @NoLogin
    public List<Map<String, Object>> getClientBigPictureList(){
        Map<String, Object> map = new HashMap<>();
        map.put("id", 1);
        map.put("title", "QQ:3346459909 群:716855350");
        map.put("imageUrl", "https://oss.icoding.ink/.inner/dnf/login_1.png");
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(map);
        return list;
    }
}

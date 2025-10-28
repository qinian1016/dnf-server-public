package com.aiyi.game.dnfserver.controller;

import com.aiyi.core.exception.ValidationException;
import com.aiyi.core.util.thread.ThreadUtil;
import com.aiyi.game.dnfserver.dao.AccountVODao;
import com.aiyi.game.dnfserver.entity.AccountVO;
import com.aiyi.game.dnfserver.entity.equipment.Equipment;
import com.aiyi.game.dnfserver.entity.stackable.Stackable;
import com.aiyi.game.dnfserver.pvf.PvfCache;
import com.aiyi.game.dnfserver.pvf.PvfManager;
import com.aiyi.game.dnfserver.utils.MinFieldUtil;
import com.aiyi.game.dnfserver.utils.cache.CacheUtil;
import com.aiyi.game.dnfserver.utils.cache.Key;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pvf相关接口
 * @author gsk
 */
@RestController
@RequestMapping("api/v1/pvf")
public class PvfController {

    @Resource
    private AccountVODao accountVODao;

    @Resource
    private PvfManager pvfManager;

    @GetMapping("info")
    private Map<String, Object> getPvfInfo() {
        HashMap<String, Object> result = new HashMap<>();
        AccountVO accountVO = accountVODao.get(ThreadUtil.getUserId());
        result.put("canUpload", accountVO.isAdmin());
        int size = PvfCache.getSize();
        if (0 == size){
            size = MinFieldUtil.getFileSizeKB(new File("Script.pvf").getAbsolutePath());
            PvfCache.setPvfSize(size);
            List<Equipment> equipmentList = pvfManager.getEquipmentList();
            PvfCache.setEquipmentList(equipmentList);
            List<Stackable> stackableList = pvfManager.getStackableList();
            PvfCache.setStackableList(stackableList);
        }
        String fileSizeStr = size + " KB";
        if (size > 1024) {
            fileSizeStr = String.format("%.2f MB", size / 1024.0);
        }
        result.put("size", fileSizeStr);
        result.put("equipmentCount", PvfCache.getEquipmentList().size());
        result.put("equipmentList", PvfCache.getEquipmentList());
        result.put("itemCount", PvfCache.getStackableList().size());
        result.put("itemList", PvfCache.getStackableList());
        return result;
    }

    @PostMapping("upload")
    public void upload(MultipartFile file){
        AccountVO accountVO = accountVODao.get(ThreadUtil.getUserId());
        if (!accountVO.isAdmin()){
            throw new ValidationException("您不能上传PVF");
        }
        // 临时保存
        CacheUtil.lock(20000, Key.as("pvf_upload_lock"), () -> {
            MinFieldUtil.cpyFile("Script.pvf", "Script_backup.pvf");
            try {
                MinFieldUtil.writeFile("Script.pvf", file.getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                pvfManager.init();
                int size = MinFieldUtil.getFileSizeKB(new File("Script.pvf").getAbsolutePath());
                List<Equipment> equipmentList = pvfManager.getEquipmentList();
                List<Stackable> stackableList = pvfManager.getStackableList();
                PvfCache.setPvfSize(size);
                PvfCache.setEquipmentList(equipmentList);
                PvfCache.setStackableList(stackableList);
            } catch (Exception e){
                // 恢复备份文件
                MinFieldUtil.cpyFile("Script_backup.pvf", "Script.pvf");
                pvfManager.init();
                throw new ValidationException("PVF文件解析失败，已恢复为原文件，请检查后重新上传！错误信息：" + e.getMessage());
            }
            return null;
        });
    }
}

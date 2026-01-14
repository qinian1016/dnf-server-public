package com.aiyi.game.dnfserver.entity.equipment;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.aiyi.game.dnfserver.entity.common.Item;

/**
 * 装备实体
 * @author xiatian
 */
public class Equipment extends Item {

    /**
     * 装备类型
     */
    private EquipmentType equipmentType;

    private String equipmentTypeStr;

    public static Equipment forScript(JSONObject equipmentScript) {
        Equipment equipment = new Equipment();
        equipment.parseForScript(equipmentScript);
        JSONArray jsonArray = equipmentScript
                .getJSONArray("[equipment type]");
        if (jsonArray != null && !jsonArray.isEmpty()) {
            equipment.equipmentType = EquipmentType.forType(jsonArray.getStr(0));
        }else{
            equipment.equipmentType = EquipmentType.weapon;
        }
        return equipment;
    }

    public EquipmentType getEquipmentType() {
        return equipmentType;
    }

    public void setEquipmentType(EquipmentType equipmentType) {
        this.equipmentType = equipmentType;
    }

    public String getEquipmentTypeStr() {
        return equipmentType.getDesc();
    }
}

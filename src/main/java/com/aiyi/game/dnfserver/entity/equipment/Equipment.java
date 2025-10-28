package com.aiyi.game.dnfserver.entity.equipment;

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
        equipment.equipmentType = EquipmentType.forType(equipmentScript
                .getJSONArray("[equipment type]").getStr(0));
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

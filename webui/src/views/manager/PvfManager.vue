<script setup>
import {computed, ref} from "vue";
import api from "../../libs/api.js";
const pvfInfo = ref({
  size: "0 MB",
  equipmentCount: 0,
  equipmentList: [],
  itemCount: 0,
  itemList: [],
  questCount: 0,
  questList: [],
  npcCount: 0,
  npcList: [],
  mapCount: 0,
  mapList: [],
  canUpload: false
});

const loadPvfInfo = () => {
  api.get("pvf/info").then(res => {
    pvfInfo.value = res.data;
    equipmentPage.value.total = pvfInfo.value.equipmentList.length;
    stackablePage.value.total = pvfInfo.value.itemList.length;
  });
}
loadPvfInfo();

const selectPvfFile = () => {
  fileInput.value.click();
}

const fileInput = ref(null);
const handleFileUpload = (event) => {
  const file = event.target.files[0];
  if (file) {
    const data = new FormData();
    data.append("file", file);
    api.post('pvf/upload', data, {
      headers: {
        "Content-Type": "multipart/form-data"
      }
    }).then(res => {
      this.$message.success("上传成功, 所有数据已更新");
      loadPvfInfo();
    })
  }
};


const equipmentPage = ref({
  page: 1,
  size: 10,
  total: 0
})
const equipmentSearchName =  ref("");
const equipmentExplain = ref("")
const equipmentType = ref("")
const equipmentLevel = ref(0);
const thisEquipmentData = computed(() => {
  let data = pvfInfo.value.equipmentList;
  if (equipmentSearchName.value){
    data = data.filter(equipment => equipment.name.includes(equipmentSearchName.value));
  }
  if (equipmentExplain.value){
    data = data.filter(equipment => equipment.explain.includes(equipmentExplain.value));
  }
  if (equipmentType.value){
    if (equipmentType.value === 'weapon'){  // 武器
      data = data.filter(equipment => equipment.equipmentType === 'weapon');
    } else if (equipmentType.value === 'armor'){  // 防具
      data = data.filter(equipment => equipment.equipmentType === 'coat' || equipment.equipmentType === 'shoulder'
          || equipment.equipmentType === 'pants' || equipment.equipmentType === 'shoes'
          || equipment.equipmentType === 'waist');
    } else if (equipmentType.value === 'accessory'){  // 饰品
      data = data.filter(equipment => equipment.equipmentType === 'amulet' || equipment.equipmentType === 'wrist'
          || equipment.equipmentType === 'ring');
    } else if (equipmentType.value === 'title'){  // 称号
      data = data.filter(equipment => equipment.equipmentType === 'titleName');
    } else if (equipmentType.value === 'support'){ // 辅助装备
      data = data.filter(equipment => equipment.equipmentType === 'support');
    } else if (equipmentType.value === 'magic stone'){ // 魔法石
      data = data.filter(equipment => equipment.equipmentType === 'magicStone');
    } else if (equipmentType.value === 'artifact'){ // 宠物装备
      data = data.filter(equipment => equipment.equipmentType.includes('artifact'));
    } else if (equipmentType.value === 'avatar'){
      data = data.filter(equipment => equipment.equipmentType.includes('avatar'));
    }else{
      data = data.filter(equipment => equipment.equipmentType === equipmentType);// 其他类型过滤
    }
  }
  if (equipmentLevel.value){
    data = data.filter(equipment => equipment.minimumLevel === Number(equipmentLevel.value));
  }
  equipmentPage.value.total = data.length;
  equipmentPage.value.page = Math.min(equipmentPage.value.page, Math.ceil(data.length / equipmentPage.value.size) || 1);
  // 倒序的
  const start = data.length - equipmentPage.value.page * equipmentPage.value.size;
  const end = start + equipmentPage.value.size;
  return data.slice(Math.max(0, start), Math.max(0, end)).reverse();
});
const handleEquipmentPageChange = (newPage) => {
  equipmentPage.value.page = newPage;
};
const handleEquipmentSizeChange = (newSize) => {
  equipmentPage.value.size = newSize;
};

const stackableTypes = ref([]);
api.get("enums/stackable-types").then(res => {
  stackableTypes.value = res.data;
});
const stackableTypeMap = computed(() => {
  const map = {};
  stackableTypes.value.forEach(type => {
    map[type.key] = type.desc;
  });
  return map;
});
const stackableType = ref("");
const stackableSearchName = ref("");
const stackableLevel = ref(0);
const stackablePage = ref({
  page: 1,
  size: 10,
  total: 0
})
const thisStackableData = computed(() => {
  let data = pvfInfo.value.itemList;
  if (stackableSearchName.value){
    data = data.filter(item => item.name.includes(stackableSearchName.value));
  }
  if (stackableType.value){
    data = data.filter(item => item.stackableType === stackableType.value);
  }
  if (stackableLevel.value){
    data = data.filter(item => item.minimumLevel === Number(stackableLevel.value));
  }
  stackablePage.value.total = data.length;
  stackablePage.value.page = Math.min(stackablePage.value.page, Math.ceil(data.length / stackablePage.value.size) || 1);
  // 倒序的
  const start = data.length - stackablePage.value.page * stackablePage.value.size;
  const end = start + stackablePage.value.size;
  return data.slice(Math.max(0, start), Math.max(0, end)).reverse();
});
const handleStackablePageChange = (newPage) => {
  stackablePage.value.page = newPage;
};
const handleStackableSizeChange = (newSize) => {
  stackablePage.value.size = newSize;
};
</script>

<template>
<div>
  <el-breadcrumb separator="/" style="margin-bottom: 30px">
    <el-breadcrumb-item>首页</el-breadcrumb-item>
    <el-breadcrumb-item>玩家管理</el-breadcrumb-item>
    <el-breadcrumb-item>账号列表</el-breadcrumb-item>
  </el-breadcrumb>

  <div class="pvf-title">
    <div class="pvf-title-text">当前PVF信息: {{ pvfInfo.size }} | 装备数量: {{pvfInfo.equipmentCount}} 个 | 物品数量: {{pvfInfo.itemCount}} 个</div>
    <el-button type="primary" icon="el-icon-upload" :disabled="!pvfInfo.canUpload" @click="selectPvfFile">上传新PVF</el-button>
    <div class="pvf-title-input-text">注意: 修改PVF会影响全局的材料、任务等信息, 权限范围不可控, 甚至可能导致系统崩溃, 所以仅最高管理可以操作。</div>
    <input type="file" style="display: none;" ref="fileInput" @change="handleFileUpload" />
  </div>
  <div class="pvf-content">
    <el-tabs>
      <el-tab-pane label="装备列表">
        <div class="search-box">
          <el-form :inline="true" >
            <el-form-item label="类别">
              <el-select v-model="equipmentType" placeholder="选择装备类型" clearable style="width: 200px">
                <el-option label="武器" value="weapon"></el-option>
                <el-option label="防具" value="armor"></el-option>
                <el-option label="饰品" value="accessory"></el-option>
                <el-option label="称号" value="title"></el-option>
                <el-option label="辅助装备" value="support"></el-option>
                <el-option label="魔法石" value="magic stone"></el-option>
                <el-option label="宠物装备" value="artifact"></el-option>
                <el-option label="时装" value="avatar"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="名称">
              <el-input placeholder="搜索装备名称" v-model="equipmentSearchName" clearable style="width: 180px"/>
            </el-form-item>
            <el-form-item label="面板">
              <el-input placeholder="输入面板关键词模糊检索" v-model="equipmentExplain" clearable style="width: 200px" />
            </el-form-item>
            <el-form-item label="等级">
              <el-input placeholder="输入装备等级" v-model="equipmentLevel" clearable style="width: 180px" type="number" />
            </el-form-item>
          </el-form>
        </div>
        <el-table :data="thisEquipmentData">
          <el-table-column prop="id" label="ID" width="120" fixed/>
          <el-table-column prop="rarity" label="稀有度" width="80" fixed>
            <template #default="scope">
              <span v-if="scope.row.rarity === 0">普通</span>
              <span v-if="scope.row.rarity === 1">高级</span>
              <span v-if="scope.row.rarity === 2">稀有</span>
              <span v-if="scope.row.rarity === 3">神器</span>
              <span v-if="scope.row.rarity === 4">史诗</span>
              <span v-if="scope.row.rarity === 5">勇者</span>
              <span v-if="scope.row.rarity === 6">神话</span>
            </template>
          </el-table-column>
          <el-table-column prop="minimumLevel" label="等级" width="55" fixed/>
          <el-table-column prop="attachType" label="交易类型" width="80" fixed>
            <template #default="scope">
              <span v-if="scope.row.attachType === 'trade'" style="color: red">不可交易</span>
              <span v-if="scope.row.attachType === 'free'" style="color: #008cff">自由交易</span>
              <span v-if="scope.row.attachType === 'sealing'" style="color: #5b1aff">封装</span>
              <span v-if="scope.row.attachType === 'account'" style="color: orange">账号绑定</span>
              <span v-if="scope.row.attachType === 'tradeDelete'" style="color: red">无法删除</span>
              <span v-if="scope.row.attachType === 'sealingTrade'" style="color: red">不可交易</span>
            </template>
          </el-table-column>
          <el-table-column prop="stackLimit" label="携带上限" width="80" fixed/>
          <el-table-column prop="usableJobsStr" label="职业" width="120" fixed/>
          <el-table-column prop="name" label="名称" width="150" show-overflow-tooltip>
            <template #default="scope">
              <span v-if="scope.row.rarity === 0">{{scope.row.name}}</span>
              <span style="color: #15a9e8" v-if="scope.row.rarity === 1">{{scope.row.name}}</span>
              <span style="color: #4200b5" v-if="scope.row.rarity === 2">{{scope.row.name}}</span>
              <span style="color: #db00fd" v-if="scope.row.rarity === 3">{{scope.row.name}}</span>
              <span style="color: #fdb500" v-if="scope.row.rarity === 4">{{scope.row.name}}</span>
              <span style="color: #fd0000" v-if="scope.row.rarity === 5">{{scope.row.name}}</span>
              <!-- 五彩色 -->
              <span style="
                          background: -webkit-linear-gradient(45deg, #db00fd, #f474fb, #7300ff, #5ef2f6);
                          -webkit-background-clip: text;
                          -webkit-text-fill-color: transparent;
                          "
                    v-if="scope.row.rarity === 6">{{scope.row.name}}</span>
            </template>
          </el-table-column>
          <el-table-column prop="explain" show-overflow-tooltip label="面板" />
          <el-table-column prop="equipmentTypeStr" label="类型" width="120" fixed />
        </el-table>
        <el-pagination
          style="margin-top: 20px; text-align: right;"
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="equipmentPage.total"
          :page-size="equipmentPage.size"
          :current-page="equipmentPage.page"
          @size-change="handleEquipmentSizeChange"
          @current-change="handleEquipmentPageChange"></el-pagination>
      </el-tab-pane>
      <el-tab-pane label="道具列表">
        <div class="search-box">
          <el-form :inline="true" >
            <el-form-item label="类别">
              <el-select v-model="stackableType" placeholder="选择装备类型" clearable style="width: 200px">
                <el-option :label="type.desc" :value="type.key" v-for="type in stackableTypes"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="名称">
              <el-input placeholder="搜索道具名称" v-model="stackableSearchName" clearable style="width: 180px"/>
            </el-form-item>
            <el-form-item label="等级">
              <el-input placeholder="输入使用等级" v-model="stackableLevel" clearable style="width: 180px" type="number" />
            </el-form-item>
          </el-form>
        </div>
        <el-table :data="thisStackableData">
          <el-table-column prop="id" label="ID" width="120" fixed/>
          <el-table-column prop="rarity" label="稀有度" width="80" fixed>
            <template #default="scope">
              <span v-if="scope.row.rarity === 0">普通</span>
              <span v-if="scope.row.rarity === 1">高级</span>
              <span v-if="scope.row.rarity === 2">稀有</span>
              <span v-if="scope.row.rarity === 3">神器</span>
              <span v-if="scope.row.rarity === 4">史诗</span>
              <span v-if="scope.row.rarity === 5">勇者</span>
              <span v-if="scope.row.rarity === 6">神话</span>
            </template>
          </el-table-column>
          <el-table-column prop="minimumLevel" label="等级" width="55" fixed/>
          <el-table-column prop="attachType" label="交易类型" width="80" fixed>
            <template #default="scope">
              <span v-if="scope.row.attachType === 'trade'" style="color: red">不可交易</span>
              <span v-if="scope.row.attachType === 'free'" style="color: #008cff">自由交易</span>
              <span v-if="scope.row.attachType === 'sealing'" style="color: #5b1aff">封装</span>
              <span v-if="scope.row.attachType === 'account'" style="color: orange">账号绑定</span>
              <span v-if="scope.row.attachType === 'tradeDelete'" style="color: red">无法删除</span>
              <span v-if="scope.row.attachType === 'sealingTrade'" style="color: red">不可交易</span>
            </template>
          </el-table-column>
          <el-table-column prop="stackLimit" label="携带上限" width="80" fixed/>
          <el-table-column prop="usableJobsStr" label="职业" width="120" fixed/>
          <el-table-column prop="name" label="名称" width="150" show-overflow-tooltip>
            <template #default="scope">
              <span v-if="scope.row.rarity === 0">{{scope.row.name}}</span>
              <span style="color: #15a9e8" v-if="scope.row.rarity === 1">{{scope.row.name}}</span>
              <span style="color: #4200b5" v-if="scope.row.rarity === 2">{{scope.row.name}}</span>
              <span style="color: #db00fd" v-if="scope.row.rarity === 3">{{scope.row.name}}</span>
              <span style="color: #fdb500" v-if="scope.row.rarity === 4">{{scope.row.name}}</span>
              <span style="color: #fd0000" v-if="scope.row.rarity === 5">{{scope.row.name}}</span>
              <!-- 五彩色 -->
              <span style="
                          background: -webkit-linear-gradient(45deg, #db00fd, #f474fb, #7300ff, #5ef2f6);
                          -webkit-background-clip: text;
                          -webkit-text-fill-color: transparent;
                          "
                    v-if="scope.row.rarity === 6">{{scope.row.name}}</span>
            </template>
          </el-table-column>
          <el-table-column prop="stackableType" label="类型" width="120" >
            <template #default="scope">
              <span>{{ stackableTypeMap[scope.row.stackableType] }}</span>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
            style="margin-top: 20px; text-align: right;"
            background
            layout="total, sizes, prev, pager, next, jumper"
            :total="stackablePage.total"
            :page-size="stackablePage.size"
            :current-page="stackablePage.page"
            @size-change="handleStackableSizeChange"
            @current-change="handleStackablePageChange"></el-pagination>
      </el-tab-pane>
    </el-tabs>
  </div>
</div>
</template>

<style scoped>
.pvf-title-text{
  font-weight: bold;
  font-size: 16px;
  margin-bottom: 10px;
}
.pvf-title-input-text{
  margin-top: 10px;
  color: red;
  font-size: 14px;
}
.search-box{
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}
</style>

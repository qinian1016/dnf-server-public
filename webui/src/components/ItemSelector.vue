<template>
  <div class="item-selector">
    <el-popover
        v-model:visible="visible"
        trigger="click"
        placement="bottom-start"
        :width="panelWidth"
        :teleported="false"
        popper-class="item-selector-popper"
        @show="onShow"
    >
      <template #reference>
        <el-input
            :model-value="selectedItem ? selectedItem.name : (props.modelValue ?? '')"
            :placeholder="placeholder"
            clearable
            readonly
            @clear="onClear"
            :suffix-icon="visible ? ArrowUp : ArrowDown"
        />
      </template>

      <div class="panel" v-loading="loading">
        <!-- 搜索 -->
        <div class="panel-toolbar">
          <el-input
              v-model="keyword"
              placeholder="搜索名称或类型"
              clearable
              :prefix-icon="Search"
          />
        </div>

        <el-tabs v-model="activeTab" class="panel-tabs" @tab-change="onTabChange">
          <!-- 装备 -->
          <el-tab-pane label="装备" name="equipment">
            <!-- 类型标签筛选（装备） -->
            <div class="type-filter" v-if="equipmentTypeOptions.length">
              <el-check-tag
                  :checked="eqTypeFilter === 'all'"
                  @change="() => changeEqType('all')"
              >全部</el-check-tag>
              <el-check-tag
                  v-for="opt in equipmentTypeOptions"
                  :key="opt.key"
                  :checked="eqTypeFilter === opt.key"
                  @change="(v) => v && changeEqType(opt.key)"
              >{{ opt.label }}</el-check-tag>
            </div>

            <el-scrollbar height="360px">
              <template v-if="pvfInfo && pvfInfo.equipmentList">
                <div v-if="pagedEquipmentList.length" class="card-grid">
                  <div
                      v-for="e in pagedEquipmentList"
                      :key="e.id"
                      class="item-card"
                      :class="[rarityClass(e.rarity), { active: e.id === props.modelValue }]"
                      @click="select(e.id)"
                  >
                    <div class="item-name" :title="e.name">{{ e.name }}</div>
                    <div class="item-sub">
                      <!-- 类型 -->
                      <el-tag size="small" effect="plain">
                        {{ (equipmentTypes && equipmentTypes[e.equipmentType]) || e.equipmentType || '未知类型' }}
                      </el-tag>
                      <!-- 稀有度标签 -->
                      <el-tag size="small" effect="plain">
                        {{ rarityLabel(e.rarity) }}
                      </el-tag>
                      <!-- 等级 -->
                      <span class="lvl" v-if="e.minimumLevel">Lv. {{ e.minimumLevel }}</span>
                    </div>
                  </div>
                </div>
                <el-empty v-else description="无匹配的装备" />
              </template>
              <el-empty v-else description="正在加载或暂无数据" />
            </el-scrollbar>

            <div v-if="filteredEquipmentList.length" style="padding: 8px 0 4px;">
              <el-pagination
                  v-model:current-page="eqPage"
                  :page-size="pageSize"
                  :total="filteredEquipmentList.length"
                  layout="prev, pager, next"
                  background
                  small
                  hide-on-single-page
              />
            </div>
          </el-tab-pane>

          <!-- 道具 -->
          <el-tab-pane label="道具" name="item">
            <!-- 类型标签筛选（道具） -->
            <div class="type-filter" v-if="stackableTypeOptions.length">
              <el-check-tag
                  :checked="itemTypeFilter === 'all'"
                  @change="() => changeItemType('all')"
              >全部</el-check-tag>
              <el-check-tag
                  v-for="opt in stackableTypeOptions"
                  :key="opt.key"
                  :checked="itemTypeFilter === opt.key"
                  @change="(v) => v && changeItemType(opt.key)"
              >{{ opt.label }}</el-check-tag>
            </div>

            <el-scrollbar height="360px">
              <template v-if="pvfInfo && pvfInfo.itemList">
                <div v-if="pagedItemList.length" class="card-grid">
                  <div
                      v-for="it in pagedItemList"
                      :key="it.id"
                      class="item-card"
                      :class="[rarityClass(it.rarity), { active: it.id === props.modelValue }]"
                      @click="select(it.id)"
                  >
                    <div class="item-name" :title="it.name">{{ it.name }}</div>
                    <div class="item-sub">
                      <!-- 类型 -->
                      <el-tag size="small" effect="plain">
                        {{ (stackableTypes && stackableTypes[it.stackableType]) || it.stackableType || '未知类型' }}
                      </el-tag>
                      <!-- 稀有度标签 -->
                      <el-tag size="small" effect="plain">
                        {{ rarityLabel(it.rarity) }}
                      </el-tag>
                      <!-- 等级 -->
                      <span class="lvl" v-if="it.minimumLevel">Lv. {{ it.minimumLevel }}</span>
                      <!-- 携带上限 -->
                      <span class="lvl" v-if="it.stackLimit != null">上限: {{ it.stackLimit }}</span>
                    </div>
                  </div>
                </div>
                <el-empty v-else description="无匹配的道具" />
              </template>
              <el-empty v-else description="正在加载或暂无数据" />
            </el-scrollbar>

            <div v-if="filteredItemList.length" style="padding: 8px 0 4px;">
              <el-pagination
                  v-model:current-page="itemPage"
                  :page-size="pageSize"
                  :total="filteredItemList.length"
                  layout="prev, pager, next"
                  background
                  small
                  hide-on-single-page
              />
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-popover>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, ArrowDown, ArrowUp } from '@element-plus/icons-vue'
import api from '../libs/api'

const props = defineProps({
  modelValue: { type: [Number, null], default: null },
  placeholder: { type: String, default: '选择物品' },
  panelWidth: { type: Number, default: 800 },
  eagerResolve: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue', 'change'])

const visible = ref(false)
const loading = ref(false)
const activeTab = ref('equipment')
const keyword = ref('')

// 分页
const pageSize = 15
const eqPage = ref(1)
const itemPage = ref(1)

// 类型筛选（默认全部）
const eqTypeFilter = ref('all')    // equipmentType key or 'all'
const itemTypeFilter = ref('all')  // stackableType key or 'all'

const pvfInfo = ref(null)          // { equipmentList, itemList, ... }
const equipmentTypes = ref(null)   // { key: desc }
const stackableTypes = ref(null)   // { key: desc }

const loadEnums = async () => {
  if (!equipmentTypes.value) {
    const { data } = await api.get('enums/equipment-types')
    equipmentTypes.value = {}
    for (const it of data) equipmentTypes.value[it.key] = it.desc
  }
  if (!stackableTypes.value) {
    const { data } = await api.get('enums/stackable-types')
    stackableTypes.value = {}
    for (const it of data) stackableTypes.value[it.key] = it.desc
  }
}
const loadPvf = async () => {
  if (!pvfInfo.value) {
    const { data } = await api.get('pvf/info')
    pvfInfo.value = data
  }
}

/* 类型标签选项 */
const equipmentTypeOptions = computed(() => {
  if (!equipmentTypes.value) return []
  return Object.keys(equipmentTypes.value).map(k => ({ key: k, label: equipmentTypes.value[k] }))
})
const stackableTypeOptions = computed(() => {
  if (!stackableTypes.value) return []
  return Object.keys(stackableTypes.value).map(k => ({ key: k, label: stackableTypes.value[k] }))
})

/* 过滤 + 倒序（装备） */
const filteredEquipmentList = computed(() => {
  let list = pvfInfo.value?.equipmentList ?? []
  const kw = (keyword.value || '').trim()
  if (kw) {
    list = list.filter(item => item.name.includes(kw))
  }
  if (eqTypeFilter.value !== 'all') {
    list = list.filter(item => String(item.equipmentType) === String(eqTypeFilter.value))
  }
  return [...list].reverse()
})
/* 过滤 + 倒序（道具） */
const filteredItemList = computed(() => {
  let list = pvfInfo.value?.itemList ?? []
  const kw = (keyword.value || '').trim()
  if (kw) {
    list = list.filter(item => item.name.includes(kw))
  }
  if (itemTypeFilter.value !== 'all') {
    list = list.filter(item => String(item.stackableType) === String(itemTypeFilter.value))
  }
  return [...list].reverse()
})

/* 分页后的列表 */
const pagedEquipmentList = computed(() => {
  const start = (eqPage.value - 1) * pageSize
  return filteredEquipmentList.value.slice(start, start + pageSize)
})
const pagedItemList = computed(() => {
  const start = (itemPage.value - 1) * pageSize
  return filteredItemList.value.slice(start, start + pageSize)
})

/* 稀有度映射：0-6 -> 文案 */
const rarityLabel = (r) => {
  switch (Number(r)) {
    case 0: return '普通'
    case 1: return '高级'
    case 2: return '稀有'
    case 3: return '神器'
    case 4: return '史诗'
    case 5: return '远古'
    case 6: return '神话'
    default: return '未知'
  }
}

/* 弹层真正展示时加载 */
const onShow = async () => {
  try {
    loading.value = true
    await Promise.all([loadEnums(), loadPvf()])
  } catch (e) {
    console.error(e)
    ElMessage.error((e && e.message) || '加载失败')
  } finally {
    loading.value = false
  }
}

const onTabChange = () => {
  // 切换 Tab 时重置页码（不动对方 Tab 的类型筛选）
  if (activeTab.value === 'equipment') {
    eqPage.value = 1
  } else {
    itemPage.value = 1
  }
}

/* 类型切换 */
const changeEqType = (key) => {
  eqTypeFilter.value = key
  eqPage.value = 1
}
const changeItemType = (key) => {
  itemTypeFilter.value = key
  itemPage.value = 1
}

const selectedItem = computed(() => {
  if (!pvfInfo.value || props.modelValue == null) return null
  const id = props.modelValue
  return (
      pvfInfo.value.equipmentList?.find(e => e.id === id) ||
      pvfInfo.value.itemList?.find(i => i.id === id) ||
      null
  )
})

/* 稀有度色条（0-6） */
const rarityClass = r => {
  switch (Number(r)) {
    case 0: return 'rarity-normal'
    case 1: return 'rarity-blue'    // 高级
    case 2: return 'rarity-purple'  // 稀有
    case 3: return 'rarity-pink'    // 神器
    case 4: return 'rarity-gold'    // 史诗
    case 5: return 'rarity-red'     // 远古
    case 6: return 'rarity-myth'    // 神话
    default: return 'rarity-normal'
  }
}

const select = id => {
  emit('update:modelValue', id)
  emit('change', id)
  visible.value = false
}
const onClear = () => {
  emit('update:modelValue', null)
  emit('change', null)
}

/* 传入值时尝试加载（懒加载） */
const tryResolveOnDemand = async () => {
  if (props.modelValue == null || selectedItem.value) return
  try {
    await Promise.all([loadEnums(), loadPvf()])
  } catch {}
}

/* 关键词变化重置页码 */
watch(keyword, () => {
  eqPage.value = 1
  itemPage.value = 1
})

watch(() => props.modelValue, () => {
  if (!pvfInfo.value) tryResolveOnDemand()
})
onMounted(() => {
  if (props.eagerResolve) tryResolveOnDemand()
})
</script>

<style scoped>
.item-selector { width: 100%; }

.panel { width: 100%; box-sizing: border-box; background: var(--el-bg-color); }
.panel-toolbar { padding: 12px; border-bottom: 1px solid var(--el-border-color-lighter); }
.panel-tabs { padding: 8px 12px; }

/* 类型标签行 */
.type-filter {
  display: flex;
  gap: 8px;
  flex-wrap: nowrap;
  overflow-x: auto;
  padding: 6px 4px 10px;
}
.type-filter :deep(.el-check-tag) {
  white-space: nowrap;
}

.card-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 8px; }
.item-card {
  border: 1px solid var(--el-border-color-lighter);
  border-left-width: 4px;
  border-radius: 6px;
  padding: 8px;
  background: var(--el-fill-color-blank);
  cursor: pointer;
  transition: all .15s;
}
.item-card:hover { box-shadow: 0 1px 4px rgba(0,0,0,.08); transform: translateY(-1px); }
.item-card.active { border-color: var(--el-color-primary); box-shadow: 0 0 0 2px var(--el-color-primary-light-8) inset; }

.item-name { font-weight: 600; line-height: 1.2; margin-bottom: 6px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.item-sub { display: flex; align-items: center; gap: 8px; font-size: 12px; color: var(--el-text-color-secondary); }
.item-sub .lvl { color: var(--el-text-color-secondary); }

/* 稀有度色条（左侧） */
.rarity-normal { border-left-color: #bdbdbd; }
.rarity-blue   { border-left-color: #409eff; }
.rarity-purple { border-left-color: #8e68ff; }
.rarity-pink   { border-left-color: #ff7aa5; }
.rarity-gold   { border-left-color: #e6a23c; }
.rarity-red    { border-left-color: #f56c6c; }
.rarity-myth   { border-left-color: transparent; border-image: linear-gradient(180deg, #58a6ff, #a78bfa, #fb7185, #f59e0b, #ef4444) 1 1; border-left-width: 6px; }

@media (max-width: 960px) { .card-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 600px) { .card-grid { grid-template-columns: 1fr; } }
</style>

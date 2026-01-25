<script setup lang="ts">
import {computed, onMounted, onBeforeUnmount, ref, watch, nextTick} from "vue";
import Request from "../api/Request";
import {Message} from "@arco-design/web-vue";
import ItemImg from "./ItemImg.vue";

type ItemIcon = {
	path: string;
	index: number;
};

type Item = {
	id: number;
	name: string;
	rarity?: number;
	minimumLevel?: number;
	stackLimit?: number;
	type?: string;
	equipmentType?: string;
	equipmentTypeStr?: string;
	stackableType?: string;
	description?: string;
	explain?: string;
	icon?: ItemIcon | null;
};

const props = withDefaults(defineProps<{
	modelValue?: Item | null;
	placeholder?: string;
	pageSize?: number;
	disabled?: boolean;
	width?: number | string;
}>(), {
	modelValue: null,
	placeholder: "选择物品",
	pageSize: 20,
	disabled: false,
	width: 360
});

const emit = defineEmits<{
	(e: "update:modelValue", value: Item | null): void;
	(e: "change", value: Item | null): void;
}>();

const popupVisible = ref(false);
const inputValue = ref("");
const selectedItem = ref<Item | null>(props.modelValue ?? null);

// More robust than `instanceof HTMLElement` (can fail across iframes / proxies)
const isDomElement = (node: any): node is Element => {
	return !!node && typeof node === "object" && node.nodeType === 1 && typeof node.getBoundingClientRect === "function";
};

const resolveHTMLElement = (el: any): HTMLElement | null => {
	if (!el) return null;
	// Vue template ref might be a Ref object
	if (el && typeof el === "object" && "value" in el) {
		return resolveHTMLElement((el as any).value);
	}
	// direct DOM element
	if (isDomElement(el)) return el as HTMLElement;
	// Vue component instance
	if (el?.$el && isDomElement(el.$el)) return el.$el as HTMLElement;
	// Some libs expose underlying element as `el` or `ref`
	if (el?.el && isDomElement(el.el)) return el.el as HTMLElement;
	if (el?.ref && isDomElement(el.ref)) return el.ref as HTMLElement;
	return null;
};

const safeGetRect = (elLike: any): DOMRect | null => {
	const node = resolveHTMLElement(elLike);
	if (!node) return null;
	try {
		return node.getBoundingClientRect();
	} catch {
		return null;
	}
};

// Dynamically fit popup into viewport
const triggerEl = ref<unknown | null>(null);
const panelMaxHeight = ref<number>(520);
const leftScrollHeight = ref<number>(420);
const rightScrollHeight = ref<number>(420);
const MAX_PANEL_HEIGHT = 520; // hard cap (as requested)
const MIN_PANEL_HEIGHT = 260;

const PANEL_WIDTH = 614;
const VIEWPORT_SIDE_GAP = 24; // leave space to viewport edges
const POPUP_CHROME_Y = 16; // reserve for popup padding/shadow/border to avoid 1-2px overflow

const viewportWidth = ref<number>(window.innerWidth || document.documentElement.clientWidth);
const updateViewportWidth = () => {
	viewportWidth.value = window.innerWidth || document.documentElement.clientWidth;
};

// Apply height/width to the popup container itself (not only the inner panel)
const popupStyle = computed(() => {
	const w = Math.max(320, Math.min(PANEL_WIDTH, viewportWidth.value - VIEWPORT_SIDE_GAP));
	return {
		maxHeight: `${panelMaxHeight.value}px`,
		width: `${w}px`,
		overflow: "hidden"
	};
});

// DOM measured heights (more robust than constant subtraction)
const rightHeaderEl = ref<HTMLElement | null>(null);
const rightPaginationEl = ref<HTMLElement | null>(null);
const leftTitleEl = ref<HTMLElement | null>(null);

const leftTitleMarginBottom = ref(0);
const rightHeaderMarginBottom = ref(0);
const rightPaginationMarginTop = ref(0);

const leftTitleHeight = ref(0);
const rightHeaderHeight = ref(0);
const rightPaginationHeight = ref(0);

let sizeObserver: ResizeObserver | null = null;

const getMarginY = (el: HTMLElement | null) => {
	if (!el) return {top: 0, bottom: 0};
	const s = window.getComputedStyle(el);
	const mt = Number.parseFloat(s.marginTop || "0") || 0;
	const mb = Number.parseFloat(s.marginBottom || "0") || 0;
	return {top: mt, bottom: mb};
};

const measureLayoutHeights = () => {
	leftTitleHeight.value = leftTitleEl.value?.getBoundingClientRect().height || 0;
	rightHeaderHeight.value = rightHeaderEl.value?.getBoundingClientRect().height || 0;
	rightPaginationHeight.value = rightPaginationEl.value?.getBoundingClientRect().height || 0;

	leftTitleMarginBottom.value = getMarginY(leftTitleEl.value).bottom;
	rightHeaderMarginBottom.value = getMarginY(rightHeaderEl.value).bottom;
	rightPaginationMarginTop.value = getMarginY(rightPaginationEl.value).top;
};

const setupSizeObserver = () => {
	if (sizeObserver) return;
	if (typeof ResizeObserver === "undefined") return;
	sizeObserver = new ResizeObserver(() => {
		measureLayoutHeights();
		updatePopupHeights();
	});
	if (leftTitleEl.value) sizeObserver.observe(leftTitleEl.value);
	if (rightHeaderEl.value) sizeObserver.observe(rightHeaderEl.value);
	if (rightPaginationEl.value) sizeObserver.observe(rightPaginationEl.value);
};

const cleanupSizeObserver = () => {
	if (!sizeObserver) return;
	sizeObserver.disconnect();
	sizeObserver = null;
};

const updatePopupHeights = (anchorRect?: DOMRect) => {
	const rect = anchorRect ?? safeGetRect(triggerEl.value);
	if (!rect) return;
	if (typeof window === "undefined") return;

	const viewportH = window.innerHeight || document.documentElement.clientHeight;

	const marginBottom = 12;
	const translateY = 6;
	const spaceBelow = viewportH - rect.bottom - marginBottom - translateY - POPUP_CHROME_Y;
	const available = Math.max(0, spaceBelow);

	const maxH = Math.max(MIN_PANEL_HEIGHT, Math.min(MAX_PANEL_HEIGHT, Math.floor(available)));
	panelMaxHeight.value = maxH;

	// LEFT: panel has padding 12(top/bottom)
	const leftPadding = 12 + 12;
	leftScrollHeight.value = Math.max(120, maxH - leftPadding - leftTitleHeight.value - leftTitleMarginBottom.value);

	// RIGHT: panel has padding 16(top/bottom)
	const rightPadding = 16 + 16;
	const headerBlock = rightHeaderHeight.value + rightHeaderMarginBottom.value;
	const paginationBlock = rightPaginationHeight.value + rightPaginationMarginTop.value;
	rightScrollHeight.value = Math.max(120, maxH - rightPadding - headerBlock - paginationBlock);
};

const onTriggerMouseDown = (e: MouseEvent) => {
	// Always capture the real DOM element as anchor (avoids component/proxy refs)
	const current = e.currentTarget as HTMLElement | null;
	if (current) triggerEl.value = current;
	updatePopupHeights(current ? current.getBoundingClientRect() : undefined);
};

watch(popupVisible, async (visible) => {
	if (visible) {
		await nextTick();
		measureLayoutHeights();
		updatePopupHeights();
		setupSizeObserver();
		window.addEventListener("resize", onWindowViewportChange);
		window.addEventListener("scroll", onWindowViewportChange, true);
	} else {
		cleanupSizeObserver();
		window.removeEventListener("resize", onWindowViewportChange);
		window.removeEventListener("scroll", onWindowViewportChange, true);
	}
});

let rafId: number | null = null;
const onWindowViewportChange = () => {
	if (rafId != null) cancelAnimationFrame(rafId);
	rafId = requestAnimationFrame(() => {
		rafId = null;
		updateViewportWidth();
		updatePopupHeights();
	});
};

onMounted(() => {
	updateViewportWidth();
});

onBeforeUnmount(() => {
	cleanupSizeObserver();
	if (rafId != null) cancelAnimationFrame(rafId);
	window.removeEventListener("resize", onWindowViewportChange);
	window.removeEventListener("scroll", onWindowViewportChange, true);
});

const loading = ref(false);
const list = ref<Item[]>([]);
const total = ref(0);
const page = ref(1);

const hoverItem = ref<Item | null>(null);
const hoverPos = ref({x: 0, y: 0});

const categoryGroups = ref([
	{
		key: "equipment",
		label: "装备",
		children: [
			{key: "equipment_all", label: "全部", type: "equipment", subType: ""},
			{key: "weapon", label: "武器", type: "equipment", subType: "weapon"},
			{key: "titleName", label: "称号", type: "equipment", subType: "titleName"},
			{key: "coat", label: "上衣", type: "equipment", subType: "coat"},
			{key: "shoulder", label: "护肩", type: "equipment", subType: "shoulder"},
			{key: "pants", label: "裤子", type: "equipment", subType: "pants"},
			{key: "shoes", label: "鞋子", type: "equipment", subType: "shoes"},
			{key: "waist", label: "腰带", type: "equipment", subType: "waist"},
			{key: "amulet", label: "项链", type: "equipment", subType: "amulet"},
			{key: "wrist", label: "手镯", type: "equipment", subType: "wrist"},
			{key: "ring", label: "戒指", type: "equipment", subType: "ring"},
			{key: "creature", label: "宠物", type: "equipment", subType: "creature"},
			{key: "aartifactRed", label: "宠物装备-红色", type: "equipment", subType: "aartifactRed"},
			{key: "aartifactGreen", label: "宠物装备-绿色", type: "equipment", subType: "aartifactGreen"},
			{key: "aartifactBlue", label: "宠物装备-蓝色", type: "equipment", subType: "aartifactBlue"},
			{key: "support", label: "辅助装备", type: "equipment", subType: "support"},
			{key: "magicStone", label: "魔法石", type: "equipment", subType: "magicStone"},
			{key: "weaponAvatar", label: "武器(装扮)", type: "equipment", subType: "weaponAvatar"},
			{key: "auroraAvatar", label: "光环(装扮)", type: "equipment", subType: "auroraAvatar"},
			{key: "hatAvatar", label: "帽子(装扮)", type: "equipment", subType: "hatAvatar"},
			{key: "hairAvatar", label: "头发(装扮)", type: "equipment", subType: "hairAvatar"},
			{key: "breastAvatar", label: "胸部(装扮)", type: "equipment", subType: "breastAvatar"},
			{key: "faceAvatar", label: "脸部(装扮)", type: "equipment", subType: "faceAvatar"},
			{key: "waistAvatar", label: "腰部(装扮)", type: "equipment", subType: "waistAvatar"},
			{key: "coatAvatar", label: "上衣(装扮)", type: "equipment", subType: "coatAvatar"},
			{key: "pantsAvatar", label: "裤子(装扮)", type: "equipment", subType: "pantsAvatar"},
			{key: "shoesAvatar", label: "鞋子(装扮)", type: "equipment", subType: "shoesAvatar"},
			{key: "skinAvatar", label: "皮肤(装扮)", type: "equipment", subType: "skinAvatar"}
		]
	},
	{
		key: "stackable",
		label: "道具",
		children: [
			{key: "stackable_all", label: "全部", type: "stackable", subType: ""},
			{key: "waste", label: "消耗品", type: "stackable", subType: "waste"},
			{key: "material", label: "材料", type: "stackable", subType: "material"},
			{key: "recipe", label: "设计图", type: "stackable", subType: "recipe"},
			{key: "material_expert_job", label: "副职业", type: "stackable", subType: "material_expert_job"},
			{key: "quest", label: "任务道具", type: "stackable", subType: "quest"},
			{key: "booster", label: "礼盒", type: "stackable", subType: "booster"},
			{key: "feed", label: "饲料", type: "stackable", subType: "feed"},
			{key: "stackable_creature", label: "宠物", type: "stackable", subType: "creature"},
			{key: "etc", label: "杂物", type: "stackable", subType: "etc"},
			{key: "throwItem", label: "投掷物", type: "stackable", subType: "throwItem"},
			{key: "legacy", label: "罐子", type: "stackable", subType: "legacy"}
		]
	}
]);

const allCategory = {key: "all", label: "全部分类", type: "", subType: ""};
const selectedCategoryKey = ref(allCategory.key);
const selectedCategory = computed(() => {
	if (selectedCategoryKey.value === allCategory.key) return allCategory;
	for (const group of categoryGroups.value) {
		const found = group.children.find(c => c.key === selectedCategoryKey.value);
		if (found) return found;
	}
	return allCategory;
});

const groupOpen = ref<Record<string, boolean>>({
	equipment: true,
	stackable: true
});

const rarityMap: Record<number, {label: string; color: string}> = {
	0: {label: "普通", color: "#d1d5db"},
	1: {label: "高级", color: "#60a5fa"},
	2: {label: "稀有", color: "#a78bfa"},
	3: {label: "神器", color: "#ed0bf5"},
	4: {label: "传说", color: "#f97316"},
	5: {label: "史诗", color: "#facc15"},
	6: {label: "神话", color: "#ef4444"}
};

const stackableTypeLabel: Record<string, string> = {
	waste: "消耗品",
	material: "材料",
	recipe: "设计图",
	material_expert_job: "副职业",
	quest: "任务道具",
	booster: "礼盒",
	feed: "饲料",
	creature: "宠物",
	etc: "杂物",
	throwItem: "投掷物",
	legacy: "罐子"
};

const inputWidth = computed(() => typeof props.width === "number" ? `${props.width}px` : props.width);

const updateHoverPos = (event: MouseEvent) => {
	hoverPos.value = {x: event.clientX + 16, y: event.clientY + 16};
};

const getItemTypeText = (item: Item) => {
	if (item.equipmentTypeStr) return item.equipmentTypeStr;
	if (item.stackableType && stackableTypeLabel[item.stackableType]) return stackableTypeLabel[item.stackableType];
	return item.type || "";
};

const buildUrl = () => {
	const params: string[] = [];
	const keyword = inputValue.value.trim();
	const category = selectedCategory.value;
	if (keyword) params.push(`keyword=${encodeURIComponent(keyword)}`);
	if (category.type) params.push(`type=${encodeURIComponent(category.type)}`);
	if (category.subType) params.push(`subType=${encodeURIComponent(category.subType)}`);
	params.push(`page=${page.value}`);
	params.push(`pageSize=${props.pageSize}`);
	return `api/v1/pvf/search${params.length ? `?${params.join("&")}` : ""}`;
};

const load = async () => {
	if (!popupVisible.value) return;
	loading.value = true;
	try {
		const res = await Request.get(buildUrl());
		list.value = res.data?.list ?? [];
		total.value = res.data?.totalSize ?? 0;
	} catch (e: any) {
		Message.error(e?.message || "查询失败");
	} finally {
		loading.value = false;
	}
};

const onSelectItem = (item: Item) => {
	selectedItem.value = item;
	inputValue.value = item.name || "";
	emit("update:modelValue", item);
	emit("change", item);
	popupVisible.value = false;
};

const onInputChange = () => {
	selectedItem.value = null;
	page.value = 1;
	loadDebounced();
};

const onCategorySelect = (key: string) => {
	selectedCategoryKey.value = key;
	page.value = 1;
	load();
};

const onPageChange = (p: number) => {
	page.value = p;
	load();
};

let timer: number | null = null;
const loadDebounced = () => {
	if (timer) {
		clearTimeout(timer);
	}
	timer = window.setTimeout(() => {
		load();
	}, 300);
};

watch(() => props.modelValue, (val) => {
	selectedItem.value = val ?? null;
	if (val?.name) inputValue.value = val.name;
});

watch(popupVisible, (visible) => {
	if (visible) {
		page.value = 1;
		load();
	}
});

onMounted(() => {
	if (selectedItem.value?.name) inputValue.value = selectedItem.value.name;
});
</script>

<template>
	<div class="item-picker" :style="{ width: inputWidth }">
		<a-trigger
			v-model:popup-visible="popupVisible"
			trigger="click"
			position="bl"
			:popup-translate="[0, 6]"
			:unmount-on-close="false"
			:auto-fit-popup-width="false"
			:auto-fit-popup-height="true"
			:popup-style="popupStyle"
			:popup-align="{ left: 'left', top: 'bottom' }"
		>
			<div ref="triggerEl" class="trigger-anchor">
				<a-input
					v-model="inputValue"
					:placeholder="placeholder"
					:disabled="disabled"
					allow-clear
					@input="onInputChange"
					@mousedown="onTriggerMouseDown"
				/>
			</div>
			<template #content>
				<div
					class="item-picker-panel"
					:style="{ maxHeight: panelMaxHeight + 'px', height: panelMaxHeight + 'px' }"
					@mousemove="updateHoverPos"
				>
					<div class="panel-left" :style="{ height: panelMaxHeight + 'px' }">
						<div ref="leftTitleEl" class="category-title">分类</div>
						<a-scrollbar class="panel-left-scroll" :style="{ height: leftScrollHeight + 'px', overflow: 'auto' }">
							<div class="category-item" :class="{active: selectedCategoryKey === allCategory.key}" @click="onCategorySelect(allCategory.key)">
								全部分类
							</div>
							<div class="category-group" v-for="group in categoryGroups" :key="group.key">
								<div class="category-group-header" @click="groupOpen[group.key] = !groupOpen[group.key]">
									<span>{{ group.label }}</span>
									<span class="chevron">{{ groupOpen[group.key] ? "▾" : "▸" }}</span>
								</div>
								<div v-show="groupOpen[group.key]" class="category-group-body">
									<div
										v-for="child in group.children"
										:key="child.key"
										class="category-item"
										:class="{active: selectedCategoryKey === child.key}"
										@click="onCategorySelect(child.key)"
									>
										{{ child.label }}
									</div>
								</div>
							</div>
						</a-scrollbar>
					</div>
					<div class="panel-right" :style="{ height: panelMaxHeight + 'px' }">
						<div ref="rightHeaderEl" class="list-header">
							<div class="result-count">共 {{ total }} 条</div>
							<div class="hint">输入关键字筛选</div>
						</div>
						<a-scrollbar class="item-list-scroll" :class="{loading}" :style="{ height: rightScrollHeight + 'px', overflow: 'auto' }">
							<div class="item-list">
								<div v-if="loading" class="loading">加载中...</div>
								<div v-else-if="list.length === 0" class="empty">暂无物品</div>
								<div
									v-for="item in list"
									:key="item.id"
									class="item-card"
									@mouseenter="hoverItem = item"
									@mouseleave="hoverItem = null"
									@click="onSelectItem(item)"
								>
									<div class="item-icon">
										<ItemImg :icon="item.icon" :rarity="item.rarity ?? 0" />
									</div>
									<div class="item-main">
										<div
											class="item-name"
											:style="{ color: rarityMap[item.rarity ?? 0]?.color || '#f3f4f6' }"
										>
											{{ item.name }}
										</div>
										<div class="item-subtitle">
											等级 {{ item.minimumLevel ?? 1 }} · {{ getItemTypeText(item) }}
										</div>
									</div>
									<div class="item-id">#{{ item.id }}</div>
								</div>
							</div>
						</a-scrollbar>
						<div ref="rightPaginationEl" class="pagination">
							<a-pagination
								simple
								:current="page"
								:page-size="pageSize"
								:total="total"
								@change="onPageChange"
							/>
						</div>
					</div>
					<div v-if="hoverItem" class="hover-panel" :style="{ left: hoverPos.x + 'px', top: hoverPos.y + 'px' }">
						<div class="hover-panel-icon-col">
							<ItemImg :icon="hoverItem.icon" :rarity="hoverItem.rarity ?? 0" style="width: 48px; height: 48px;" />
						</div>
						<div class="hover-panel-content-col">
							<div class="hover-title">{{ hoverItem.name }}</div>
							<div class="hover-row">ID：{{ hoverItem.id }}</div>
							<div class="hover-row">类型：{{ getItemTypeText(hoverItem) }}</div>
							<div class="hover-row">稀有度：{{ rarityMap[hoverItem.rarity ?? 0]?.label || "普通" }}</div>
							<div class="hover-row">等级：{{ hoverItem.minimumLevel ?? 1 }}</div>
							<div class="hover-row">堆叠上限：{{ hoverItem.stackLimit ?? 1 }}</div>
							<div v-if="hoverItem.explain" class="hover-desc">{{ hoverItem.explain }}</div>
							<div v-else-if="hoverItem.description" class="hover-desc">{{ hoverItem.description }}</div>
						</div>
					</div>
				</div>
			</template>
		</a-trigger>
	</div>
</template>

<style scoped lang="less">
.item-picker {
	position: relative;
}

.item-picker-panel {
	width: 614px;
	// height is controlled via inline style (panelMaxHeight)
	height: 100%;
	box-sizing: border-box;
	display: flex;
	background: rgb(32, 32, 43);
	color: #e5e7eb;
	border-radius: 10px;
	box-shadow: 0 12px 30px rgba(0, 0, 0, 0.35);
	overflow: hidden;
	position: relative;
	/* Follow popup width on small screens so content isn't clipped */
	max-width: 100%;
}

/* If the viewport is narrower than 920px, allow horizontal scroll inside popup */
:deep(.arco-trigger-popup) {
	box-sizing: border-box;
	padding: 0;
	max-height: inherit;
	overflow-x: auto;
	overflow-y: hidden;
}

/* Some Arco versions wrap again or apply inner padding; neutralize it here as well */
:deep(.arco-trigger-popup-content) {
	box-sizing: border-box;
	padding: 0;
}

.panel-left {
	width: 240px;
	border-right: 1px solid #2f2f2f;
	padding: 12px;
	box-sizing: border-box;
	overflow: hidden;
}

.panel-right {
	flex: 1;
	width: auto;
	max-width: calc(100% - 240px);
	padding: 16px;
	display: flex;
	flex-direction: column;
	box-sizing: border-box;
	overflow: hidden;
	min-height: 0;
	height: 100%;
}

.category-title {
	font-size: 12px;
	color: #9ca3af;
	margin-bottom: 8px;
}

.category-group {
	margin-top: 10px;
}

.category-group-header {
	display: flex;
	justify-content: space-between;
	font-weight: 600;
	padding: 6px 8px;
	cursor: pointer;
	background: rgba(255, 255, 255, 0.06);
	border-radius: 6px;
}

.category-group-body {
	margin-top: 6px;
}

.category-item {
	padding: 6px 8px;
	border-radius: 6px;
	cursor: pointer;
	color: #d1d5db;
	margin-top: 4px;
}

.category-item.active {
	background: #334155;
	color: #fff;
}

.category-item:hover {
	background: #2d3748;
}


.list-header {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 10px;
	font-size: 12px;
	color: #9ca3af;
	flex: none;
}

.item-list {
	padding-right: 6px;
	min-height: 0;
}

.loading,
.empty {
	padding: 20px;
	text-align: center;
	color: #94a3b8;
}

.item-card {
	display: flex;
	align-items: center;
	padding: 12px;
	border-radius: 10px;
	background: #262626;
	margin-bottom: 10px;
	cursor: pointer;
	border: 1px solid transparent;
	transition: all 0.15s ease;
}

.item-id {
	font-size: 12px;
	color: #94a3b8;
	margin-left: auto;
	text-align: right;
	flex: none;
	min-width: 48px;
}

.item-icon {
	flex: none;
	width: 32px;
	height: 32px;
	display: flex;
	align-items: center;
	justify-content: center;
	margin-right: 14px;
}

.item-main {
	display: flex;
	flex-direction: column;
	justify-content: center;
	gap: 4px;
	min-width: 0;
	height: 32px;
}

.item-name {
	font-size: 14px;
	font-weight: 600;
	color: #f3f4f6;
	white-space: nowrap;
	overflow: hidden;
	text-overflow: ellipsis;
	flex: 1;
	min-width: 0;
	line-height: 1.2;
}

.item-subtitle {
	font-size: 12px;
	color: #b5b8c5;
	white-space: nowrap;
	overflow: hidden;
	text-overflow: ellipsis;
	line-height: 1.2;
}

.item-id {
	font-size: 12px;
	color: #94a3b8;
	margin-left: auto;
	text-align: right;
	flex: none;
	min-width: 48px;
}

.pagination {
	display: flex;
	justify-content: flex-end;
	margin-top: 10px;
	flex: none;
}

.hover-panel {
	position: fixed;
	z-index: 9999;
	background: rgba(17, 24, 39, 0.96);
	color: #e5e7eb;
	padding: 12px;
	border-radius: 8px;
	box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
	max-width: 340px;
	pointer-events: none;
	display: flex;
	flex-direction: row;
	align-items: flex-start;
	min-width: 220px;
}

.hover-panel-icon-col {
	flex: none;
	margin-right: 16px;
	display: flex;
	align-items: flex-start;
	justify-content: center;
	width: 48px;
	height: 48px;
}

.hover-panel-content-col {
	flex: 1;
	min-width: 0;
	display: flex;
	flex-direction: column;
	justify-content: flex-start;
}

.hover-title {
	font-weight: 600;
	margin-bottom: 6px;
}

.hover-row {
	font-size: 12px;
	color: #cbd5f5;
	margin-bottom: 4px;
}

.hover-desc {
	margin-top: 6px;
	font-size: 12px;
	color: #e2e8f0;
	line-height: 1.4;
}

.panel-left-scroll {
	min-height: 0;
}

.item-list-scroll {
	min-height: 0;
}
</style>


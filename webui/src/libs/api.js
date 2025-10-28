import axios from "axios";
import router from "../router/index.js";
import { ElMessage } from "element-plus";

/**
 * axios 全局配置
 */
axios.defaults.timeout = 20000;
axios.defaults.headers.post["Content-Type"] = "application/json;charset=UTF-8";
axios.defaults.baseURL = "api/v1";

/**
 * ====== /pvf/info 会话内内存缓存 ======
 * - 仅缓存 GET /pvf/info
 * - 会话内永久缓存（无 TTL 过期），直到：
 *   1) 浏览器/标签页关闭或刷新，或
 *   2) 发生 /pvf/* 的写操作（POST/PUT/PATCH/DELETE）触发清空
 * - 支持：
 *   - 强制刷新：config.meta?.force === true
 *   - 禁用缓存：config.meta?.cache === false
 */
const PVF_CACHE_MAX = 100; // 最多缓存 100 条（防止内存无限增长）
const pvfCache = new Map();

/** 判断是否 /pvf/info（忽略 baseURL） */
function isPVFInfo(url = "") {
    return /(^|\/)pvf\/info(\/|\?|$)/.test(url);
}

/** 判断是否 /pvf/*（用于写操作清缓存） */
function isPVFAny(url = "") {
    return /(^|\/)pvf\/(.*)/.test(url);
}

/** 稳定序列化 params（排序键，确保 key 稳定） */
function stableStringify(obj) {
    if (obj === null || typeof obj !== "object") return JSON.stringify(obj);
    if (Array.isArray(obj)) return "[" + obj.map(stableStringify).join(",") + "]";
    return (
        "{" +
        Object.keys(obj)
            .sort()
            .map((k) => JSON.stringify(k) + ":" + stableStringify(obj[k]))
            .join(",") +
        "}"
    );
}

/** 仅对 GET /pvf/info 生成缓存 key（按 params 维度区分） */
function makeKey(config) {
    return JSON.stringify({
        u: "/pvf/info",
        p: config.params ? stableStringify(config.params) : "",
    });
}

/** 简易 LRU：超上限删除最早的 */
function setCache(key, response) {
    if (pvfCache.size >= PVF_CACHE_MAX) {
        const firstKey = pvfCache.keys().next().value;
        pvfCache.delete(firstKey);
    }
    pvfCache.set(key, { response });
}
function getCache(key) {
    return pvfCache.get(key);
}

/**
 * 请求拦截器
 */
axios.interceptors.request.use(
    (config) => {
        // 允许携带 cookie 与通用头
        config.withCredentials = true;
        config.headers["Content-Type"] = "application/json";
        config.headers["token"] = window.localStorage.getItem("token");

        const method = (config.method || "get").toLowerCase();

        // 写操作命中 /pvf/* 时，清空缓存（避免脏读）
        if (["post", "put", "patch", "delete"].includes(method) && isPVFAny(config.url)) {
            pvfCache.clear();
        }

        // 仅对 GET /pvf/info 生效
        if (method === "get" && isPVFInfo(config.url)) {
            const enableCache = config.meta?.cache !== false; // 默认启用缓存
            const forceRefresh = config.meta?.force === true; // 强制刷新忽略缓存

            if (enableCache && !forceRefresh) {
                const key = makeKey(config);
                const cached = getCache(key);
                if (cached) {
                    // 命中缓存：使用 adapter 短路返回
                    const cachedResp = cached.response;
                    config.adapter = async (cfg) => {
                        // 保持 axios 响应结构，更新 config 引用
                        return Promise.resolve({
                            ...cachedResp,
                            config: cfg,
                            __fromCache: true,
                        });
                    };
                }
            }
        }

        return config;
    },
    (error) => Promise.reject(error)
);

/**
 * 响应拦截器
 */
axios.interceptors.response.use(
    (response) => {
        if (response.status === 200) {
            const cfg = response.config || {};
            const method = (cfg.method || "get").toLowerCase();
            const enableCache = cfg.meta?.cache !== false;

            // 将 GET /pvf/info 写入缓存（即使是强制刷新请求）
            if (method === "get" && isPVFInfo(cfg.url) && enableCache && !response.__fromCache) {
                const key = makeKey(cfg);
                setCache(key, response);
            }

            return Promise.resolve(response);
        } else {
            return Promise.reject(response);
        }
    },
    (error) => {
        // 服务器状态码不是 200 的情况
        console.log(error);
        try {
            if (error.response?.status) {
                const status = error.response.status;
                if (status === 401) {
                    router.replace({ path: "/login" });
                } else if (status === 400) {
                    ElMessage.error(error.response.data?.message || "请求参数错误");
                } else {
                    ElMessage.error("网络错误，请稍后再试");
                }
                return Promise.reject(error.response);
            }
        } catch (e) {
            ElMessage.error("网络错误，请稍后再试");
            return Promise.reject(error.response || error);
        }
        // 无 response 的异常（网络错误等）
        ElMessage.error("网络错误，请稍后再试");
        return Promise.reject(error);
    }
);

export default axios;

/**
 * 使用示例：
 * - 默认（会话内永久缓存）：axios.get('/pvf/info')
 * - 强制刷新：axios.get('/pvf/info', { meta: { force: true } })
 * - 禁用缓存：axios.get('/pvf/info', { meta: { cache: false } })
 */

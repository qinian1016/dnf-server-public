<template>
    <div class="home">
        <div class="nav-con">
            <nav-bar/>
        </div>

        <div style="text-align: center;">
            <img src="../assets/imgs/banner.jpg" style="width: 100%; position: fixed; z-index: -1; left: 0;">
        </div>

        <div style="text-align: center">
            <h1 style="font-size: 50px; color: white; margin-top: 100px">60版本，经典再现</h1>
            <div style="color: white; text-shadow: 2px 2px 3px #2c3e50; margin-top: 60px;">本游戏来自台湾Garena公司停止运营后的最后一个版本，主要针对台服玩家，与中国腾讯无任何直接关系。</div>
            <div style="color: white; text-shadow: 2px 2px 3px #2c3e50; margin-top: 30px;">如果您是国服用户，请自觉删除，以避免相关纠纷。</div>
            <a href="https://oss.icoding.ink/.inner/dnf/dnf.zip" class="download-btn">下载游戏</a>
            <a href="javascript:;" @click="go('/register')" class="download-btn">账号注册</a>
        </div>

    </div>
</template>

<script>

import NavBar from "../components/NavBar.vue";
import router from "../router/index.js";

export default {
    name: 'Home',
    components: {NavBar},
    methods: {
      go(path){
        router.push({
          path: path
        })
      }
    }
}
</script>

<style scoped lang="less">
    .nav-con{
        background-color: rgb(84, 92, 100);
        border-bottom: solid 1px #e6e6e6;
        height: 60px;
    }
    .download-btn{
        display: inline-block;
        padding: 15px 20px;
        background-color: rgba(255, 255, 255, .2);
        outline: none;
        color: white;
        text-decoration:none;
        border: solid 1px white;
        border-radius: 10px;
        margin: 100px 20px;
        transition: background-color,color 1s;
        -webkit-transition: background-color,color 1s; /* Safari */
    }
    .download-btn:hover{
        background-color: rgba(255, 255, 255, .9);
        color: gray;
    }
</style>

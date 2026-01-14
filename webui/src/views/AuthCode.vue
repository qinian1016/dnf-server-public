<script setup>
import {computed, ref} from "vue";
import api from "../libs/api.js";

const form = ref({
  password: ''
})
const result = ref('')
const submit = () => {
  api.post('authcode', form.value).then(res => {
    result.value = res.data.authCode;
  })
}
const finalResult = computed(() => {
  // 每隔4个字符添加一个空格，提升可读性
  if (!result.value) return '';
  let text = result.value.replace(/(.{4})/g, '$1-').trim();
  // 大写
  text = text.toUpperCase();
  return text;
})
const copyToClipboard = (text) => {
  navigator.clipboard.writeText(text).then(() => {
    alert('授权码已复制到剪贴板');
  }, () => {
    alert('复制失败，请手动复制');
  });
}
</script>

<template>
<div class="auth-code">
  <div class="title">授权码</div>
  <el-form class="form">
    <el-form-item label="管理密码">
      <el-input placeholder="输入临时密码" v-model="form.password" type="password"></el-input>
    </el-form-item>
    <div class="center">
      <el-button type="primary" @click="submit">生成授权码</el-button>
    </div>
    <div class="result" v-if="result">
      <h3>授权码：</h3>
      <div style="margin-bottom: 10px">{{finalResult}}</div>
      <el-button size="small" @click="copyToClipboard(result)">复制授权码</el-button>
    </div>
  </el-form>
</div>
</template>

<style scoped>
.auth-code{
  .title{
    background: #1a2940;
    color: white;
    line-height: 30px;
    font-size: 20px;
    text-align: center;
    margin-bottom: 10px;
  }
  .form{
    padding: 10px;
  }
  .center{
    text-align: center;
  }
  .result{
    margin-top: 20px;
    padding: 10px;
    border: solid 1px #e6e6e6;
    background: #f5f5f5;
  }
}
</style>

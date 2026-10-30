<template>
  <!-- 基于 Element UI 新增和修改弹窗 -->
  <el-dialog
    :title="!dataForm.id ? '新增' : '编辑'"
    :close-on-click-modal="false"
    :visible.sync="visible">
    <!-- 新增和创建表单表单 -->
    <el-form :model="dataForm" :rules="dataRule" ref="dataForm" @keyup.enter.native="dataSubmit()" label-width="80px">
    <el-form-item label="用户id" prop="uid">
      <el-input v-model="dataForm.uid" placeholder="用户id"></el-input>
    </el-form-item>
    <el-form-item label="门店" prop="storeId">
      <el-select v-model="dataForm.storeId" placeholder="请选择"  class="selWidth" clearable filterable>
        <el-option
        :value="item.id" v-for="(item, index) in storeSelectList" :key="index" :label="item.name"
          >
        </el-option>
      </el-select>
    </el-form-item>
    <el-form-item label="手艺人ID" prop="artisanId">
      <el-input v-model="dataForm.artisanId" placeholder="手艺人ID"></el-input>
    </el-form-item>
    <el-form-item label="预约时间" prop="appointmentDate">
      <el-input v-model="dataForm.appointmentDate" placeholder="预约时间"></el-input>
    </el-form-item>
    <el-form-item label="商品ID" prop="productIds">
      <el-input v-model="dataForm.productIds" placeholder="商品ID字符串"></el-input>
    </el-form-item>
    <el-form-item label="核销码" prop="verifyCode">
      <el-input v-model="dataForm.verifyCode" placeholder="核销码"></el-input>
    </el-form-item>
    <el-form-item label="状态">
        <el-radio-group v-model="dataForm.status">
          <el-radio :label="1">开启</el-radio>
          <el-radio :label="0">关闭</el-radio>
        </el-radio-group>
    </el-form-item>
    <!-- <el-form-item label="创建时间" prop="createTime">
      <el-input v-model="dataForm.createTime" placeholder="创建时间"></el-input>
    </el-form-item>
    <el-form-item label="更新时间" prop="updateTime">
      <el-input v-model="dataForm.updateTime" placeholder="更新时间"></el-input>
    </el-form-item> -->
    </el-form>
    <span slot="footer" class="dialog-footer">
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="dataSubmit()">确定</el-button>
    </span>
  </el-dialog>
</template>

<script>
  import { appointmentsCreate, appointmentsDetail } from "@/api/appointments"
  import {storeListApi } from '@/api/storePoint'
  export default {
    data () {
      return {
        visible: false,
        storeSelectList:[],
        dataForm: {
          id: 0,
          uid: '' , 
          storeId: '' , 
          artisanId: '' , 
          appointmentDate: '' , 
          productIds: '' , 
          verifyCode: '' , 
          status: '' , 
          createTime: '' , 
          updateTime: '' 
        },
        dataRule: {
          uid: [
            { required: true, message: '用户id  为必填项', trigger: 'blur' }
          ],
          storeId: [
            { required: true, message: '门店id  为必填项', trigger: 'blur' }
          ],
          artisanId: [
            { required: true, message: '手艺人ID  为必填项', trigger: 'blur' }
          ],
          appointmentDate: [
            { required: true, message: '预约时间  为必填项', trigger: 'blur' }
          ],
          productIds: [
            { required: true, message: '商品ID字符串  为必填项', trigger: 'blur' }
          ],
          verifyCode: [
            { required: true, message: '核销码  为必填项', trigger: 'blur' }
          ],
          status: [
            { required: true, message: '1为正常，0为禁止  为必填项', trigger: 'blur' }
          ],
          createTime: [
            { required: true, message: '创建时间  为必填项', trigger: 'blur' }
          ],
          updateTime: [
            { required: true, message: '更新时间  为必填项', trigger: 'blur' }
          ]
        }
      }
    },
    methods: {
      init (id) { // 初始化表单验证规则
        this.dataForm.id = id || 0
        this.visible = true
        this.storeList();
        this.$nextTick(() => {
          this.$refs['dataForm'].resetFields()
          if (this.dataForm.id) {
            appointmentsDetail(id).then(res => {
                this.dataForm = res;
            })
          }
        })
      },
      storeList() {
        let artFrom =  {
            page: 1,
            limit: 9999,
            status: '1',
            keywords: ''
        };
        storeListApi(artFrom).then(res=>{
          this.storeSelectList = res.list;
        })
      },
      // 表单数据提交
      dataSubmit () {
        this.$refs['dataForm'].validate((valid) => {
          if (valid) {
                  appointmentsCreate().then(res =>{
                      // TODO 保存数据
                  });
          }
        })
      }
    }
  }
</script>
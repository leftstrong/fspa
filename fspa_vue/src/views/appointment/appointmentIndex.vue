<template>
  <div class="divBox">
    <el-form :inline="true" :model="dataForm" @keyup.enter.native="getDataList()">
      <!-- <el-form-item>
        <el-button type="primary" @click="addOrUpdateHandle()">新增</el-button>
      </el-form-item> -->
       <el-form-item label="门店：">
        <el-select v-model="dataForm.storeId" placeholder="请选择"  class="selWidth" clearable filterable @change="getDataList">
          <el-option
          :value="item.id" v-for="(item, index) in storeSelectList" :key="index" :label="item.name"
            >
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="核销码">
              <el-input v-model="dataForm.verifyCode" placeholder="请输入核销码" class="selWidth" size="small" clearable>
                <el-button slot="append" icon="el-icon-search" @click="getDataList" size="small"/>
              </el-input>
              <label> 预约时间：
                  <el-date-picker @change="onchangeTime" v-model="timeVal" value-format="yyyy-MM-dd"  format="yyyy-MM-dd"  size="small" type="daterange" placement="bottom-end" placeholder="自定义时间" style="width: 220px;"></el-date-picker>
              </label>

        </el-form-item>
    </el-form>
    <el-table
      :data="dataList"
      border
      v-loading="dataListLoading"
      @selection-change="selectionChangeHandle"
      style="width: 100%;">
      <el-table-column
        type="selection"
        header-align="center"
        align="center"
        width="50">
      </el-table-column>
      <!-- <el-table-column
        prop="id"
        header-align="center"
        align="center"
        label="ID">
      </el-table-column> -->
      <el-table-column
        prop="storeName"
        header-align="center"
        align="center"
        label="门店">
      </el-table-column>
      <el-table-column
        prop="uname"
        header-align="center"
        align="center"
        label="用户名称">
      </el-table-column>
      <el-table-column
        prop="artisanName"
        header-align="center"
        align="center"
        label="手艺人名称">
      </el-table-column>
      <el-table-column
        prop="verifyCode"
        header-align="center"
        align="center"
        label="核销码">
      </el-table-column>
      <el-table-column
        prop="productNames"
        header-align="center"
        align="center"
        label="商品名称">
      </el-table-column>
      <el-table-column
        prop="statusName"
        header-align="center"
        align="center"
        label="状态">
      </el-table-column>
      <el-table-column
        prop="appointmentDate"
        header-align="center"
        align="center"
        label="预约时间">
      </el-table-column>
      <el-table-column
        prop="createTime"
        header-align="center"
        align="center"
        label="创建时间">
      </el-table-column>
      <!-- <el-table-column
        prop="updateTime"
        header-align="center"
        align="center"
        label="更新时间">
      </el-table-column> -->
      <el-table-column
        fixed="right"
        header-align="center"
        align="center"
        width="150"
        label="操作">
        <template slot-scope="scope">
          <!-- <el-button type="text" size="small" @click="addOrUpdateHandle(scope.row.id)">修改</el-button> -->
          <el-button type="text" size="small" @click="deleteHandle(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      @size-change="sizeChangeHandle"
      @current-change="currentChangeHandle"
      :current-page="pageIndex"
      :page-sizes="[10, 20, 50, 100]"
      :page-size="pageSize"
      :total="totalPage"
      layout="total, sizes, prev, pager, next, jumper">
    </el-pagination>
    <!-- 表单弹窗, 新增数据和修改数据 -->
    <add-or-update v-if="addOrUpdateVisible" ref="addOrUpdate" @refreshDataList="getDataList"></add-or-update>
  </div>
</template>

<script>
  // import * as api from './.appointments.js'
  import AddOrUpdate from './appointmentEdit'
  import { appointmentsList, appointmentsDelete } from "@/api/appointments";
  import {storeListApi } from '@/api/storePoint'
  export default {
    data () {
      return {
       dataForm: {
          storeId: '',
          verifyCode: '',
          page: 1,
          limit: 10,
          dateLimit: '',
        },
        timeVal: [],
        storeSelectList:[],
        dataList: [],
        pageIndex: 1,
        pageSize: 10,
        totalPage: 0,
        dataListLoading: false,
        dataListSelections: [],
        addOrUpdateVisible: false
      }
    },
    components: {
      AddOrUpdate
    },
    mounted() {
      this.getDataList();
      this.storeList();
    },
    methods: {
      // 获取数据列表
      getDataList () {
        this.dataListLoading = true
          appointmentsList(this.dataForm).then(res => {
            // TODO 获取数据列表
            this.dataList = res.list;
            this.totalPage = res.total;
            this.dataListLoading = false
          }).catch(() => {
          this.dataListLoading = false
        })
      },
      // 每页数
      sizeChangeHandle (val) {
        this.dataForm.limit = val
        this.dataForm.page = 1
        this.getDataList()
      },
      // 当前页
      currentChangeHandle (val) {
        this.dataForm.page = val
        this.getDataList()
      },
      // 具体日期
     onchangeTime (e) {
      this.timeVal = e;
      this.dataForm.dateLimit = e ? this.timeVal.join(',') : ''
      this.dataForm.page = 1
      this.getDataList();
      },
      // 多选
      selectionChangeHandle (val) {
        this.dataListSelections = val
      },
      // 新增 / 修改
      addOrUpdateHandle (id) {
        this.addOrUpdateVisible = true
        this.$nextTick(() => {
          this.$refs.addOrUpdate.init(id)
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
      // 删除
      deleteHandle (id) {
        const pram = {
          id: id
        }
      this.$confirm('确实删除当前数据', '提示').then(() => {
        appointmentsDelete(pram).then(data => {
          this.$message.success('删除数据成功')
          this.getDataList();
        })
      })
      }
    }
  }
</script>
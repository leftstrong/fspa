<template>
  <div class="divBox relative">
    <el-card class="box-card">
      <div slot="header" class="clearfix">
        <!--        <el-tabs v-model="tableFrom.type" @tab-click="seachList" v-if="checkPermi(['admin:product:tabs:headers'])">-->
        <!--          <el-tab-pane :label="item.name +'('+item.count +')' " :name="item.type.toString()" v-for="(item,index) in headeNum" :key="index"/>-->
        <!--        </el-tabs>-->
        <el-tabs v-model="activeTabName" @tab-click="handleSelectTab">
          <el-tab-pane label="出售中商品" name="product">
            <el-card class="box-card">
              <div slot="header" class="clearfix">
                <div class="container mt-1">
                  <el-form inline size="small">
                    <el-form-item label="商品分类：">
                      <el-cascader v-model="tableFrom.cateId" :options="merCateList" :props="props" clearable
                                   class="selWidth mr20" @change="seachList" size="small"/>
                    </el-form-item>
                    <el-form-item label="商品搜索：">
                      <el-input v-model="tableFrom.keywords" placeholder="请输入商品名称，关键字，商品ID" class="selWidth"
                                size="small"
                                clearable>
                        <el-button slot="append" icon="el-icon-search" @click="seachList" size="small"
                                   v-hasPermi="['admin:product:list']"/>
                      </el-input>
                    </el-form-item>
                  </el-form>
                </div>
              </div>
              <el-table
                v-loading="listLoading"
                empty-text="空空如也"
                :data="tableData.data"
                style="width: 100%"
                size="mini"
                :highlight-current-row="true"
                :header-cell-style=" {fontWeight:'bold'}"
              >
                <el-table-column type="expand">
                  <template slot-scope="props">
                    <el-form label-position="left" inline class="demo-table-expand">
                      <el-form-item label="商品分类：">
                      <span v-for="(item, index) in props.row.cateValues.split(',')" :key="index" class="mr10">{{
                          item
                        }}</span>
                      </el-form-item>
                      <el-form-item label="市场价：">
                        <span>{{ props.row.otPrice }}</span>
                      </el-form-item>
                      <el-form-item label="成本价：">
                        <span>{{ props.row.cost }}</span>
                      </el-form-item>
                      <el-form-item label="收藏：">
                        <span>{{ props.row.collectCount }}</span>
                      </el-form-item>
                      <el-form-item label="虚拟销量：">
                        <span>{{ props.row.ficti }}</span>
                      </el-form-item>
                    </el-form>
                  </template>
                </el-table-column>
                <el-table-column
                  prop="id"
                  label="ID"
                  min-width="50"
                />
                <el-table-column label="商品图" min-width="80">
                  <template slot-scope="scope">
                    <div class="demo-image__preview">
                      <el-image
                        style="width: 36px; height: 36px"
                        :src="scope.row.image"
                        :preview-src-list="[scope.row.image]"
                      />
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="商品名称"
                                 prop="storeName"
                                 min-width="300"
                                 :show-overflow-tooltip="true">
                </el-table-column>
                <el-table-column
                  prop="price"
                  label="商品售价"
                  min-width="90"
                  align="center"
                />
                <el-table-column
                  prop="sales"
                  label="销量"
                  min-width="90"
                  align="center"
                />
                <el-table-column
                  prop="stock"
                  label="库存"
                  min-width="90"
                  align="center"
                />
                <el-table-column label="操作" min-width="150" fixed="right" align="center">
                  <template slot-scope="scope">
                    <router-link :to="{path: '/store/list/creatProduct/' + scope.row.id + '/1'}">
                      <el-button type="text" size="small" class="mr10" v-hasPermi="['admin:product:info']">详情
                      </el-button>
                    </router-link>
                    <router-link :to="{path: '/store/list/creatProduct/' + scope.row.id}">
                      <el-button type="text" size="small" class="mr10"
                                 v-if="tableFrom.type !== '5' && tableFrom.type !== '1'"
                                 v-hasPermi="['admin:product:update']">编辑
                      </el-button>
                    </router-link>
                    <el-button v-if="tableFrom.type === '5'" type="text" size="small"
                               @click="handleRestore(scope.row.id, scope.$index)"
                               v-hasPermi="['admin:product:restore']">
                      恢复商品
                    </el-button>
                    <el-button type="text" size="small" @click="handleAddCart(scope.row)"
                               v-hasPermi="['admin:product:delete']">{{ '加入购物车' }}
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
            <div class="block">
              <el-pagination
                :page-sizes="[20, 40, 60, 80]"
                :page-size="tableFrom.limit"
                :current-page="tableFrom.page"
                layout="total, sizes, prev, pager, next, jumper"
                :total="tableData.total"
                @size-change="handleSizeChange"
                @current-change="pageChange"
              />
            </div>
            <!--商品属性选择弹框-->
            <el-dialog v-loading="attrLoading" center :close-on-click-modal="false" title="选择属性规格"
                       :before-close="selectAttrCloseBefore"
                       :visible.sync="selectAttrVisible">
              <template>
                <el-table
                  ref="multipleTable"
                  :data="selectAttr.productValue"
                  tooltip-effect="dark"
                  style="width: 100%"
                  highlight-current-row
                  @current-change="handleSelectionAttrChange">
                  <!--                    <el-table-column
                                        type="selection"
                                        width="55">
                                      </el-table-column>-->
                  <el-table-column label="规格" width="200">
                    <template slot-scope="scope">
                      <span>{{ scope.row.sukKey }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column v-for="(column,index) in attrColumns"
                                   :key="index"
                                   :prop="column.prop"
                                   :label="column.lable"
                                   :width="column.width">
                    <template slot-scope="scope">
                      <div v-if="column.prop ==='value.image'" class="demo-image__preview">
                        <el-image
                          style="width: 36px; height: 36px"
                          :src="scope.row.value.image"
                          :preview-src-list="[scope.row.value.image]"
                        />
                      </div>
                      <span v-else>{{ scope.row.value[column.key] }}</span>
                    </template>
                  </el-table-column>
                </el-table>
              </template>
              <el-card>
                <el-row :gutter="8">
                  <el-col :span="2" :offset="10">
                    <el-button @click.stop="selectAttrCloseBefore" type="info" autocomplete="off">取消
                    </el-button>
                  </el-col>
                  <el-col :span="2">
                    <el-button @click.stop="selectAttrAddCart" type="primary" autocomplete="off">确定
                    </el-button>
                  </el-col>
                </el-row>
              </el-card>
            </el-dialog>
          </el-tab-pane>
          <el-tab-pane label="购物车" name="shopCart">
            <el-card>
              <div slot="header" class="clearfix">
                <el-row>
                  <el-col :span="4">
                    <el-input v-model="userPhone" @focus="userRealName=''" placeholder="输入用户手机号"></el-input>
                  </el-col>
                  <el-col :span="4">
                    <el-button type="primary" @click="getUserInfo">查询</el-button>
                  </el-col>
                  <el-col :span="4">
                    <el-form :inline="true" class="demo-form-inline">
                      <el-form-item label="用户姓名:">
                        <el-input v-text="userRealName" placeholder="用户姓名"></el-input>
                      </el-form-item>
                    </el-form>
                  </el-col>
                </el-row>
              </div>
              <el-table
                empty-text="空空如也"
                v-loading="cartProductListLoading"
                :data="cartTableData.data"
                style="width: 100%"
                size="mini"
                :highlight-current-row="true"
                :header-cell-style=" {fontWeight:'bold'}"
              >
                <el-table-column label="商品图" min-width="80">
                  <template slot-scope="scope">
                    <div class="demo-image__preview">
                      <el-image
                        style="width: 36px; height: 36px"
                        :src="scope.row.image"
                        :preview-src-list="[scope.row.image]"
                      />
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="商品名称"
                                 prop="storeName"
                                 min-width="300"
                                 :show-overflow-tooltip="true"
                >
                </el-table-column>
                <el-table-column
                  prop="suk"
                  label="商品规格"
                  min-width="90"
                  align="center"
                ></el-table-column>
                <el-table-column
                  prop="price"
                  label="商品售价"
                  min-width="90"
                  align="center"
                />
                <el-table-column
                  prop="cartNum"
                  label="数量"
                  min-width="90"
                  align="center"
                >
                  <template slot-scope="scope">
                    <el-input-number :disabled="!scope.row.attrStatus"
                                     size="small" @change="handleChangeCarNum(scope.row)"
                                     v-model="scope.row.cartNum"
                                     :min="1" :max="scope.row.stock"></el-input-number>
                  </template>
                </el-table-column>
                <el-table-column label="操作" min-width="150" fixed="right" align="center">
                  <template slot-scope="scope">
                    <router-link :to="{path: '/store/list/creatProduct/' + scope.row.id + '/1'}">
                      <el-button type="text" size="small" class="mr10">详情</el-button>
                    </router-link>
                    <el-button type="text" size="small" class="mr10" @click="removeProduct(scope.row)">移出购物车
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
            <div class="block">
              <el-pagination
                :page-sizes="[20, 40, 60, 80]"
                :page-size="cartTableFrom.limit"
                :current-page="cartTableFrom.page"
                layout="total, sizes, prev, pager, next, jumper"
                :total="cartTableData.total"
                @size-change="handleCarSizeChange"
                @current-change="cartPageChange"
              />
            </div>
            <el-card>
              <div slot="header" class="clearfix">
                <!--                <span style="color: #e93323;font-size: large;float: right; padding: 3px 0">¥{{ '999.00' }}</span>-->
                <el-row :gutter="12">
                  <el-col style="color: #e93323;font-size: large" :span="4" :offset="22">
                    ¥{{ totalAmount }}
                  </el-col>
                </el-row>
              </div>
              <el-row :gutter="12" v-loading="orderButtonLoading">
                <el-col :span="2" :offset="20">
                  <el-button type="info" @click="cleanShopCart">清空购物车</el-button>
                </el-col>
                <el-col :span="2">
                  <el-button @click="createPreOrder" type="danger">立即下单</el-button>
                </el-col>
              </el-row>
              <!--              <el-card v-if="preOrderNo">
                              <el-row :gutter="12">
                                <el-col :span="2" :offset="20">
                                  <el-button type="info" @click="cleanShopCart">清空购物车</el-button>
                                </el-col>
                                <el-col :span="2">
                                  <el-button type="danger">立即结算</el-button>
                                </el-col>
                              </el-row>
                            </el-card>-->
            </el-card>
          </el-tab-pane>
          <el-tab-pane label="订单" name="third">
            <order-index></order-index>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-card>
    <el-dialog
      title="复制淘宝、天猫、京东、苏宁"
      :visible.sync="dialogVisible"
      :close-on-click-modal="false"
      width="1200px"
      class="taoBaoModal"
      :before-close="handleClose">
      <tao-bao v-if="dialogVisible" @handleCloseMod="handleCloseMod"></tao-bao>
    </el-dialog>
  </div>
</template>

<script>
import {categoryApi, productExcelApi, productLstApi, restoreApi} from '@/api/store'
import {getProductDetail, getUserInfoByPhone} from '@/api/shop'
import {changeCartNum, cleanCart, getCartList, postCartAdd, removeProductForCart} from '@/api/shopCart'
import {checkPermi} from "@/utils/permission";
import orderIndex from "@/views/shop/orderIndex";
import {shopOrderCreatApi} from "@/api/shopOrder";

const attrColumns = [
  {key: 'image', prop: 'value.image', lable: '图片', width: ''},
  {key: 'price', prop: 'value.price', lable: '售价', width: ''},
  {key: 'cost', prop: 'value.cost', lable: '成本价', width: ''},
  {key: 'otPrice', prop: 'value.otPrice', lable: '原价', width: ''},
  {key: 'stock', prop: 'value.stock', lable: '库存', width: ''},
  {key: 'weight', prop: 'value.weight', lable: '重量(KG)', width: ''},
  {key: 'volume', prop: 'value.volume', lable: '体积(m³)', width: ''},
]
export default {
  name: 'Shop',
  components: {
    orderIndex
  },
  data() {
    return {
      props: {
        children: 'child',
        label: 'name',
        value: 'id',
        emitPath: false
      },
      // 商品
      listLoading: true,
      // 购物车商品
      cartProductListLoading: true,
      // 商品表格
      tableData: {
        data: [],
        total: 0
      },
      tableFrom: {
        page: 1,
        limit: 20,
        cateId: '',
        keywords: '',
        type: '1'
      },
      // 购物车商品表格
      cartTableData: {
        data: [],
        total: 0
      },
      cartTableFrom: {
        page: 1,
        limit: 20,
        cateId: '',
        keywords: '',
        type: '1',
        isValid: true
      },
      categoryList: [],
      merCateList: [],
      objectUrl: process.env.VUE_APP_BASE_API,
      dialogVisible: false,
      activeTabName: 'product',
      // 商品属性选择弹框标识
      selectAttrVisible: false,
      // 商品属性选择表单
      selectAttrForm: {},
      selectAttr: {},
      attrColumns: attrColumns,
      // 商品属性加载标识
      attrLoading: false,
      // 选中的属性
      selectAttrRow: {},
      // 总金额
      totalAmount: 0,
      userPhone: '',
      // 用户真实姓名
      userRealName: '',
      // 预下单信息
      preOrderNo: '',
      // 门店列表
      shopList: [],
      orderButtonLoading: false
    }
  },
  mounted() {
    this.getList()
    this.getCartProductList()
    this.getCategorySelect()
    this.checkedCities = this.$cache.local.has('goods_stroge') ? this.$cache.local.getJSON('goods_stroge') : this.checkedCities;
  },
  methods: {
    checkPermi,
    handleSelectTab(tab, event) {
      if (this.activeTabName === "product") {
        this.getList();
      }
      if (this.activeTabName === "shopCart") {
        this.getCartProductList();
      }
    },
    handleRestore(id) {
      this.$modalSure("恢复商品").then(() => {
        restoreApi(id)
          .then((res) => {
            this.$message.success('操作成功');
            this.goodHeade();
            this.getList();
          })
      });
    },
    seachList() {
      this.tableFrom.page = 1
      this.getList()
    },
    handleClose() {
      this.dialogVisible = false
    },
    handleCloseMod(item) {
      this.dialogVisible = item
      this.getList();
    },
    // 复制
    onCopy() {
      this.dialogVisible = true
    },
    // 导出
    exports() {
      productExcelApi({
        cateId: this.tableFrom.cateId,
        keywords: this.tableFrom.keywords,
        type: this.tableFrom.type
      }).then((res) => {
        window.location.href = res.fileName;
      })
    },
    // 商户分类；
    getCategorySelect() {
      categoryApi({status: -1, type: 1}).then(res => {
        this.merCateList = res
      }).catch(res => {
        this.$message.error(res.message)
      })
    },
    // 商品列表
    getList() {
      this.listLoading = true
      productLstApi(this.tableFrom).then(res => {
        this.tableData.data = res.list
        this.tableData.total = res.total
        this.listLoading = false
      }).catch(res => {
        this.listLoading = false
        this.$message.error(res.message)
      })
    },
    pageChange(page) {
      this.tableFrom.page = page
      this.getList()
    },
    handleSizeChange(val) {
      this.tableFrom.limit = val
      this.getList()
    },
    // 购物车商品列表
    getCartProductList() {
      this.cartProductListLoading = true
      getCartList(this.cartTableFrom).then(res => {
        let list = res.list
        this.cartTableData.data = res.list
        this.cartTableData.total = res.total
        this.cartProductListLoading = false
        let total = 0;
        list.forEach(item => {
          total += item.price * item.cartNum
        })
        this.totalAmount = parseFloat(total + '');
      }).catch(err => {
        this.cartProductListLoading = false
        this.$message.error(err.message)
      })
    },
    cartPageChange(page) {
      this.cartTableFrom.page = page
      this.getList()
    },
    handleCarSizeChange(val) {
      this.cartTableFrom.limit = val
      this.getList()
    },
    // 点击添加购物车
    handleAddCart(raw) {
      let arrtLen = raw.attrValue.length
      // 只有默认属性直接加入购物车
      if (arrtLen === 1) {
        let attr = {
          id: raw.attrValue[0].id
        }
        let param = {
          id: raw.id,
          attr: [attr],
          stock: raw.stock
        }
        this.joinCart(param, 1)
      } else {
        // 有多个属性则打开弹窗选择
        this.selectProductAttr(raw)
      }
    },
    // 选择属性后添加到购物车
    selectAttrAddCart() {
      this.selectAttrCloseBefore()
      let attrValue = this.selectAttrRow.value;
      const attr = {
        id: attrValue.id
      }
      const param = {
        stock: attrValue.stock,
        id: attrValue.productId,
        attr: [attr]
      }
      this.joinCart(param, 1)
    },
    /**
     * 获取产品详情
     *
     */
    getGoodsDetails: function (product) {
      let that = this
      that.attrLoading = true
      let params = {
        id: product.id,
        type: product.type
      }
      getProductDetail(params).then(res => {
        that.selectAttr = res;
        that.attrLoading = false
      })
    },
    /**
     * 打开属性加入购物车
     *
     */
    joinCart: function (param, num) {
      this.goCat(param, num);
    },
    // 加入购物车
    goCat: function (param, num) {
      let that = this
      that.listLoading = true;
      // let productSelect = that.productValue[this.attrValue];
      //打开属性
      /*      if (that.attrValue) {
              //默认选中了属性，但是没有打开过属性弹窗还是自动打开让用户查看默认选中的属性
              that.attr.cartAttr = !that.isOpen ? true : false;
            } else {
              if (that.isOpen) that.attr.cartAttr = true;
              else that.attr.cartAttr = !that.attr.cartAttr;
            }
            //只有关闭属性弹窗时进行加入购物车
            if (that.attr.cartAttr === true && that.isOpen === false) {
              return (that.isOpen = true);
            }*/
      //如果有属性,没有选择,提示用户选择
      if (param.stock === 0) {
        return that.$message.error('产品库存不足，请选择其它')
      }
      let q = {
        productId: parseFloat(param.id),
        cartNum: parseFloat(num),
        isNew: false,
        productAttrUnique: param.attr[0].id
      };
      postCartAdd(q).then(function (res) {
        // that.isOpen = false;
        // that.attr.cartAttr = false;
        that.listLoading = false;
        return that.$message.success('添加购物车成功')
        /*          that.$util.Tips({
                    title: "添加购物车成功",
                    success: () => {
                      that.getCartCount(true);
                    }
                  });*/
      })
        .catch(res => {
          // that.isOpen = false;
          /*            return that.$util.Tips({
                        title: res
                      });*/
          that.listLoading = false;
          return that.$message.error(res)
        });
    },
    // 修改购物车商品数量
    handleChangeCarNum(param) {
      this.ChangeCartNum(param)
    },
    // 购物车数量加和数量减
    ChangeCartNum: function (param) {
      const data = {
        id: param.id,
        cartNum: param.cartNum,
        productAttrId: param.attrId
      }
      changeCartNum(data).then(res => {
        this.$message.success("修改商品数量成功")
        this.getCartProductList()
      }).catch(err => {
        this.$message.error("修改商品数量错误" + err)
        this.getCartProductList()
      })
    },
    // 清空购物车
    cleanShopCart(param) {
      this.$confirm(`确定清空购物车?, 是否继续?`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.cartProductListLoading = true
        cleanCart().then(res => {
          this.cartProductListLoading = false
          this.$message.success("清空购物车成功")
          this.getCartProductList()
        }).catch(err => {
          this.cartProductListLoading = false
          this.$message.error("清空购物车失败")
          this.getCartProductList()
        })
      }).catch(() => {
        this.$message({
          type: 'info',
          message: '已取消'
        });
      });
    },
    removeProduct(param) {
      this.$confirm(`确定将${param.storeName}移出购物车?, 是否继续?`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.cartProductListLoading = true
        const data = {
          id: param.id
        }
        removeProductForCart(data).then(res => {
          this.cartProductListLoading = false
          this.$message.success("移出购物车成功")
          this.getCartProductList()
        }).catch(err => {
          this.cartProductListLoading = false
          this.$message.error("移出购物车失败")
          this.getCartProductList()
        })
      }).catch(() => {
        this.$message({
          type: 'info',
          message: '已取消'
        });
      });
    },
    // 打开选择商品属性弹框
    selectProductAttr(param) {
      let that = this
      that.selectAttrVisible = true
      this.getGoodsDetails(param)
    },
    // 关闭选择商品属性弹框
    selectAttrCloseBefore() {
      this.selectAttrVisible = false;
    },
    // 选中属性监听
    handleSelectionAttrChange(val) {
      this.selectAttrRow = val
    },
    // 查询用户信息
    getUserInfo() {
      const phone = this.userPhone
      console.log(phone)
      const pattern = /^(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\d{8}$/;
      if (!pattern.test(phone)) {
        return this.$message.error("手机号格式错误")
      }
      if (!phone) {
        return this.$message.error("查询手机号不能为空")
      }
      const param = {
        phone: phone
      }
      getUserInfoByPhone(param).then(res => {
        this.userRealName = res
      }).catch(err => {
        this.$message.error(err)
      })
    },
    // 立即下单---预下单
    createPreOrder() {
      this.orderButtonLoading = true
      const orderDetails = this.cartTableData.data.map(item => {
        return {
          shoppingCartId: item.id
        }
      });
      if (!orderDetails) {
        return this.$message.error("购物车无商品无法下单!")
      }
      const param = {
        preOrderRequest: {
          orderDetails: orderDetails,
        },
        realName: this.userRealName,
        phone: this.userPhone,
      }
      shopOrderCreatApi(param).then(res => {
        this.orderButtonLoading = false
        this.getCartProductList();
        this.$message.success("下单成功")
      }).catch(err => {
        this.$message.error(err)
        this.orderButtonLoading = false
        this.getCartProductList();
      })
    },
  }
}
</script>

<style scoped lang="scss">
.el-table__body {
  width: 100%;
  table-layout: fixed !important;
}

.taoBaoModal {
  //  z-index: 3333 !important;
}

.demo-table-expand {
  ::v-deep label {
    width: 82px;
  }
}

.demo-table-expand {
  ::v-deep .el-form-item__content {
    width: 77%;
  }
}

.selWidth {
  width: 350px !important;
}

.seachTiele {
  line-height: 30px;
}

.relative {
  position: relative;
}
</style>

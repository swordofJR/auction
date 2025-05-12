<template>
  <div class="vm-image-list">
    <Row class="image-list-heading vm-panel">
      <div class="panel-heading">
        {{ title }}
      </div>
      <Row type="flex" align="middle" justify="space-between" class="panel-body">
       <div class="search-bar">
          <Input placeholder="Please enter ..." v-model="keyword" style="width: 300px"></Input>
          <Button type="ghost" @click="search"><i class="fa fa-search"></i></Button>
        </div>
        <Row type="flex" align="middle" class="page">
          <span>展示</span>
          <Input :max="40" :min="1" :number="true" v-model="showNum" class="input-number" @on-change=" updateDataShow "></Input>
          <span class="margin-end">/ 页</span>
          <span class="total">合计 {{ data.length }}</span>
          <Page :total="data.length" :current="currentPage" :page-size="showNum" @on-change="pageChange"></Page>
        </Row>
      </Row>
    </Row>
    <Row class="image-list" :gutter="16">
      <Col :lg="6" :sm="12" class="vm-margin" v-for="item in dataShow" :key="item.id">
        <VmCard 
          :editable="true" 
          :title="item.title" 
          :img="item.img" 
          :desc="item.desc" 
          :detailUrl="item.detailUrl"
          :price="item.price"
          :ownerAddress="item.ownerAddress"
          :category="item.category"
          :status="item.status"
          :username="item.username"
          :id="item.id"
          :isAdmin="isAdmin"
          :attachmentPaths="item.attachmentPaths"
          @edit="handleEdit(item)"
          @delist="handleDelist(item)">
        </VmCard>
      </Col>
    </Row>

    <!-- Auction Dialog -->
    <Modal v-model="showAuctionDialog" title="确认购买" @on-ok="handleAuctionConfirm" :ok-text="loading ? '处理中...' : '确认购买'" :loading="loading" :maskClosable="!loading" :closable="!loading">
      <div class="auction-dialog-content">
        <div class="item-info">
          <img :src="selectedItem.img" class="item-image">
          <div class="item-details">
            <h3>{{ selectedItem.title }}</h3>
            <p>{{ selectedItem.desc }}</p>
            <p v-if="selectedItem.price"><strong>价格: </strong>{{ selectedItem.price }} ETH</p>
            <p><strong>类别: </strong>{{ selectedItem.category || '未分类' }}</p>
          </div>
        </div>
        <div class="auction-form">
          <Form :model="auctionForm" :label-width="100">
            <FormItem label="出价金额">
              <InputNumber v-model="auctionForm.bidAmount" :min="selectedItem.price" :step="0.1" placeholder="请输入出价金额"></InputNumber>
            </FormItem>
            <p class="bidding-note">注: 出价必须大于或等于标价</p>
          </Form>
        </div>
      </div>
    </Modal>
  </div>
</template>

<script>
  import VmCard from '@/components/vm-card'
  import axios from 'axios'
  import Web3 from 'web3'
  import { abi } from '../contracts/CopyrightNFT.json'
  import { contractAddress } from '../contracts/config'
  
  export default {
    name: 'VmImageList',
    components: {
      VmCard
    },
    props: {
      title: {
        type: String,
        default: '数字藏品交易'
      },
      // origin data
      data: {
        type: Array,
        default: function () {
          return [
            {
              id: '19920805',
              title: 'Title',
              img: require('@/assets/img/img-1.jpg'),
              desc: 'Lorem Ipsum is simply dummy text of the printing and typesetting industry,Lorem Ipsum has been the industry\'s standard dummy text ever since the 1500s ly dummy tly dummy tly dummy tly dummy tly dummy tly dummy t',
              to: '#'
            }
          ]
        }
      },
      isAdmin: {
        type: Boolean,
        default: false
      }
    },
    data: function () {
      return {
        keyword: '', // keyword for search
        dataShow: [], // data for showing
        showNum: 8, // number of item per page
        currentPage: 1,
        showAuctionDialog: false,
        selectedItem: {},
        auctionForm: {
          bidAmount: 0.01
        },
        web3: null,
        contract: null,
        loading: false
      }
    },
    async created() {
      await this.initWeb3()
    },
    methods: {
      async initWeb3() {
        if (window.ethereum) {
          try {
            // 请求用户授权
            await window.ethereum.request({ method: 'eth_requestAccounts' })
            this.web3 = new Web3(window.ethereum)
            // 初始化合约
            this.contract = new this.web3.eth.Contract(abi, contractAddress)
          } catch (error) {
            console.error('Error initializing web3:', error)
          }
        } else {
          console.warn('MetaMask not detected')
        }
      },
      updateDataShow: function () {
        let startPage = (this.currentPage - 1) * this.showNum
        let endPage = startPage + this.showNum
        this.dataShow = this.data.slice(startPage, endPage)
      },
      pageChange: function (page) {
        this.currentPage = page
        this.updateDataShow()
      },
      search: function () {
        let that = this
        let tempData = that.data
        that.dataShow = []
        tempData.forEach(function (elem) {
          for (let i in elem) {
            if (elem[i].toString().indexOf(that.keyword) > -1) {
              that.dataShow.push(elem)
              return
            }
          }
        })
      },
      // deleteOk: function (data) {
      //   this.$emit('delete-ok', data)
      // },
      handleEdit: function(item) {
        this.selectedItem = item
        this.auctionForm.bidAmount = item.price || 0.01
        this.showAuctionDialog = true
      },
      async handleAuctionConfirm() {
        // 获取当前用户信息
        const userStr = sessionStorage.getItem('user')
        if (!userStr) {
          this.$Message.warning('请先登录')
          return false
        }
        const user = JSON.parse(userStr)
        // 验证用户不能购买自己的藏品
        // if (this.selectedItem.ownerAddress === user.address ||
        //     (this.selectedItem.userId && user.id && this.selectedItem.userId === user.id)) {
        //   this.$Message.warning('不能购买自己发布的数字藏品')
        //   this.showAuctionDialog = false
        //   return false
        // }
        if (!this.auctionForm.bidAmount || this.auctionForm.bidAmount < this.selectedItem.price) {
          this.$Message.warning('出价必须大于或等于标价')
          return false // 返回false阻止Modal关闭
        }
        this.loading = true
        // 区块链交易结果标志
        let blockchainSuccess = false
        try {
          // 1. 准备基础数据
          const accounts = await this.web3.eth.getAccounts()
          const account = accounts[0]
          // 准备交易元数据
          const metadata = {
            id: this.selectedItem.id,
            title: this.selectedItem.title,
            price: this.auctionForm.bidAmount,
            seller: this.selectedItem.ownerAddress,
            buyer: account
          }
          // 2. 尝试区块链交易，但即使失败也继续进行数据库操作
          try {
            if (!this.web3 || !this.contract) {
              await this.initWeb3()
              if (!this.web3 || !this.contract) {
                console.warn('MetaMask钱包未连接，将跳过区块链记录')
              }
            }
            if (this.web3 && this.contract) {
              const etherValue = this.web3.utils.toWei(this.auctionForm.bidAmount.toString(), 'ether')
              console.log('尝试区块链交易...')
              // 尝试检查合约中实际存在的方法
              const methodNames = Object.keys(this.contract.methods)
              console.log('合约中可用的方法:', methodNames)
              // mint方法通常是最可靠的
              if (methodNames.includes('mint')) {
                console.log('找到 mint 方法，正在调用...')
                await this.contract.methods
                  .mint(account, JSON.stringify(metadata))
                  .send({
                    from: account,
                    value: etherValue
                  })
                console.log('mint 方法调用成功')
                blockchainSuccess = true
              } else if (methodNames.includes('buyToken')) {
                console.log('找到 buyToken 方法，正在调用...')
                await this.contract.methods
                  .buyToken(this.selectedItem.id, JSON.stringify(metadata))
                  .send({
                    from: account,
                    value: etherValue
                  })
                console.log('buyToken 方法调用成功')
                blockchainSuccess = true
              } else if (methodNames.includes('purchase') || methodNames.includes('purchaseToken')) {
                const method = methodNames.includes('purchase') ? 'purchase' : 'purchaseToken'
                console.log(`找到 ${method} 方法，正在调用...`)
                await this.contract.methods[method](this.selectedItem.id, JSON.stringify(metadata))
                  .send({
                    from: account,
                    value: etherValue
                  })
                console.log(`${method} 方法调用成功`)
                blockchainSuccess = true
              } else if (methodNames.includes('buy') || methodNames.includes('buyCopyright')) {
                const method = methodNames.includes('buy') ? 'buy' : 'buyCopyright'
                console.log(`找到 ${method} 方法，正在调用...`)
                await this.contract.methods[method](this.selectedItem.id, JSON.stringify(metadata))
                  .send({
                    from: account,
                    value: etherValue
                  })
                console.log(`${method} 方法调用成功`)
                blockchainSuccess = true
              } else {
                console.warn('找不到合约中适合的方法，将跳过区块链记录')
              }
            }
          } catch (blockchainError) {
            console.error('区块链交易失败:', blockchainError)
            // 区块链交易失败，继续数据库操作
          }
          // 3. 无论区块链是否成功，都更新数据库（如果不是原始藏品）
          if (!this.selectedItem.isOriginal) {
            // 从sessionStorage获取用户信息和用户ID
            const userStr = sessionStorage.getItem('user')
            let newUserId = null
            if (userStr) {
              try {
                const user = JSON.parse(userStr)
                newUserId = user.id
                console.log('购买者用户ID:', newUserId)
              } catch (e) {
                console.error('解析用户信息失败:', e)
              }
            }
            if (!newUserId) {
              this.$Message.warning('获取用户ID失败，无法完成购买')
              this.loading = false
              return false
            }
            await axios.post(`/api/jdbc/copyright/${this.selectedItem.id}/purchase`, null, {
              params: {
                newOwnerAddress: account,
                newUserId: newUserId
              }
            })
            console.log(`版权 ${this.selectedItem.id} 状态已更新为 SOLD，新拥有者: ${account}`)
            this.$Message.success(blockchainSuccess
              ? `购买成功！${this.selectedItem.title} 状态已更新为 SOLD，交易已记录到区块链`
              : `购买成功！${this.selectedItem.title} 状态已更新为 SOLD，但未能记录到区块链`)
          } else {
            // 对于原始藏品，只记录交易，不更新数据库
            console.log(`原始藏品 ${this.selectedItem.id} 交易已${blockchainSuccess ? '' : '尝试'}记录到区块链`)
            this.$Message.success(blockchainSuccess
              ? `购买成功！${this.selectedItem.title} 已记录到区块链`
              : `购买成功！${this.selectedItem.title} 处理完成，但未能记录到区块链`)
          }
          // 4. 从显示列表中移除已购买项
          this.data = this.data.filter(item => item.id !== this.selectedItem.id)
          this.updateDataShow()
          this.showAuctionDialog = false
          // 5. 触发购买成功事件，通知父组件刷新数据
          this.$emit('purchase-success', {
            itemId: this.selectedItem.id,
            buyer: account,
            buyerId: user && user.id ? user.id : null
          })
        } catch (error) {
          console.error('Purchase failed:', error)
          this.$Message.error('购买失败: ' + (error.message || '未知错误'))
          return false
        } finally {
          this.loading = false
        }
      },
      handleDelist: function(item) {
        this.$Modal.confirm({
          title: '确认下架',
          content: `确定要下架藏品 "${item.title}" 吗？下架后将不会显示在交易市场中。`,
          onOk: async () => {
            try {
              // 调用下架API
              await axios.post(`/api/jdbc/copyright/${item.id}/delist`)
              // 从显示列表中移除该项
              this.data = this.data.filter(i => i.id !== item.id)
              this.updateDataShow()
              // 触发下架成功事件
              this.$emit('delist-success', {
                itemId: item.id,
                title: item.title
              })
              // 显示成功信息
              this.$Message.success(`已成功下架藏品 "${item.title}"`)
            } catch (error) {
              console.error('下架失败:', error)
              this.$Message.error('下架藏品失败: ' + (error.message || '未知错误'))
            }
          }
        })
      }
    },
    watch: {
      data: function () {
        this.dataShow = this.data.slice(0, this.showNum) // update dataShow once data changed
      }
    },
    mounted: function () {
      this.dataShow = this.data.slice(0, this.showNum)
    }
  }
</script>

<style scoped>
.auction-dialog-content {
  padding: 20px;
}
.item-info {
  display: flex;
  margin-bottom: 20px;
}
.item-image {
  width: 120px;
  height: 120px;
  object-fit: cover;
  margin-right: 20px;
}
.item-details {
  flex: 1;
}
.item-details h3 {
  margin: 0 0 10px 0;
}
.item-details p {
  margin: 0;
  color: #666;
}
.auction-form {
  margin-top: 20px;
}

.bidding-note {
  color: #ff9900;
  font-size: 12px;
  margin-top: 5px;
  padding-left: 100px;
}
</style>

<template>
  <div class="marketplace vm-margin">
    <h1 class="page-title">拍卖市场</h1>
    <div class="filter-bar">
      <Input v-model="searchQuery" placeholder="搜索竞品..." style="width: 200px" />
      <Select v-model="categoryFilter" style="width: 150px; margin-left: 10px;">
        <Option value="">全部类别</Option>
        <Option v-for="(cat, index) in categories" 
               :value="cat" 
               :key="index">{{ cat }}</Option>
      </Select>
      <Select v-model="sortOption" style="width: 150px; margin-left: 10px;">
        <Option value="newest">最新上架</Option>
        <Option value="endingSoon">即将结束</Option>
        <Option value="priceLow">价格从低到高</Option>
        <Option value="priceHigh">价格从高到低</Option>
      </Select>
    </div>

    <div class="items-grid">
      <Card v-for="item in filteredItems" :key="item.id" class="item-card">
        <div class="item-image" @click="openItemDetails(item)">
          <img :src="require('../assets/img/bg.jpg')" alt="竞品图片" />
        </div>
        <div class="item-info">
          <h3 class="item-title">{{ item.title }}</h3>
          <div class="item-details">
            <p class="item-price">{{ item.price }} ETH</p>
            <p class="item-time">
              <Icon type="ios-time-outline" />
              {{ getTimeRemaining(item.auctionEndTime) }}
            </p>
          </div>
          <div class="item-category">
            <Tag>{{ item.category }}</Tag>
          </div>
          <Button type="primary" long @click="openItemDetails(item)">查看详情</Button>
        </div>
      </Card>

      <div v-if="filteredItems.length === 0" class="no-items">
        <Icon type="ios-alert-outline" size="48" />
        <p>暂无匹配的竞品</p>
      </div>
    </div>

    <!-- 竞品详情弹窗 -->
    <Modal v-model="detailsModal" width="700" :title="selectedItem ? selectedItem.title : '竞品详情'">
      <div v-if="selectedItem" class="item-details-modal">
        <div class="item-details-image">
          <img :src="require('../assets/img/bg.jpg')" alt="竞品图片" />
        </div>
        <div class="item-details-info">
          <h2>{{ selectedItem.title }}</h2>
          <div class="seller-info">
            <span><strong>卖家:</strong> {{ selectedItem.username || '未知用户' }}</span>
          </div>
          <Divider />
          <div class="price-section">
            <h3>当前价格</h3>
            <div class="current-price">{{ selectedItem.price }} ETH</div>
          </div>
          <div class="time-section">
            <h3>拍卖剩余时间</h3>
            <div class="time-remaining">{{ getTimeRemaining(selectedItem.auctionEndTime) }}</div>
          </div>
          <Divider />
          <div class="description-section">
            <h3>竞品描述</h3>
            <p>{{ selectedItem.description }}</p>
          </div>
          <div class="category-section">
            <h3>类别</h3>
            <Tag color="blue">{{ selectedItem.category }}</Tag>
          </div>
          <div class="attachments-section" v-if="selectedItem.attachmentPaths">
            <h3>附件</h3>
            <ul class="attachment-list">
              <li v-for="(attachment, index) in getAttachments(selectedItem.attachmentPaths)" :key="index">
                <a @click="downloadAttachment(attachment)" class="attachment-link">
                  <Icon type="ios-document" /> {{ getAttachmentName(attachment) }}
                </a>
              </li>
            </ul>
          </div>
          <div class="bid-section">
            <InputNumber v-model="bidAmount" :min="getMinBidAmount()" :step="0.01" placeholder="出价金额 (ETH)" style="width: 200px" />
            <Button type="primary" :loading="bidding" :disabled="!canPlaceBid" @click="placeBid">
              {{ bidding ? '处理中...' : '出价' }}
            </Button>
          </div>
        </div>
      </div>
      <div slot="footer">
        <Button @click="detailsModal = false">关闭</Button>
      </div>
    </Modal>
  </div>
</template>

<script>
import axios from 'axios'
import Web3 from 'web3'
import { abi } from '../contracts/CopyrightNFT.json'
import { contractAddress } from '../contracts/config'

export default {
  name: 'Marketplace',
  data() {
    return {
      items: [],
      categories: ['艺术品', '收藏品', '电子产品', '宠物', '游戏', '生活', '旅行'],
      searchQuery: '',
      categoryFilter: '',
      sortOption: 'newest',
      detailsModal: false,
      selectedItem: null,
      bidAmount: null,
      bidding: false,
      web3: null,
      contract: null,
      currentUser: null,
      refreshInterval: null
    }
  },
  computed: {
    filteredItems() {
      let result = [...this.items]
      // 应用搜索过滤
      if (this.searchQuery) {
        const query = this.searchQuery.toLowerCase()
        result = result.filter(item =>
          item.title.toLowerCase().includes(query) ||
          item.description.toLowerCase().includes(query)
        )
      }
      // 应用类别过滤
      if (this.categoryFilter) {
        result = result.filter(item => item.category === this.categoryFilter)
      }
      // 应用排序
      switch (this.sortOption) {
        case 'newest':
          result.sort((a, b) => new Date(b.updatedTime) - new Date(a.updatedTime))
          break
        case 'endingSoon':
          result.sort((a, b) => new Date(a.auctionEndTime) - new Date(b.auctionEndTime))
          break
        case 'priceLow':
          result.sort((a, b) => a.price - b.price)
          break
        case 'priceHigh':
          result.sort((a, b) => b.price - a.price)
          break
      }
      return result
    },
    canPlaceBid() {
      if (!this.selectedItem || !this.bidAmount || !this.currentUser) {
        return false
      }
      // 检查出价是否高于当前价格
      if (this.bidAmount <= this.selectedItem.price) {
        return false
      }
      // 检查是否已过期
      const endTime = new Date(this.selectedItem.auctionEndTime)
      if (endTime < new Date()) {
        return false
      }
      // 检查是否是自己的竞品
      if (this.currentUser.id === this.selectedItem.userId) {
        return false
      }
      return true
    }
  },
  async created() {
    await this.initWeb3()
    // 从sessionStorage获取用户信息
    const userStr = sessionStorage.getItem('user')
    if (userStr) {
      try {
        this.currentUser = JSON.parse(userStr)
      } catch (e) {
        console.error('解析用户信息失败:', e)
      }
    }
  },
  mounted() {
    this.loadMarketplaceItems()
    // 设置定时刷新，每30秒更新一次数据
    this.refreshInterval = setInterval(() => {
      this.refreshData()
    }, 30000)
  },
  beforeDestroy() {
    // 组件销毁前清除定时器
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval)
    }
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
          console.error('初始化Web3失败:', error)
        }
      } else {
        console.warn('未检测到MetaMask插件')
      }
    },
    loadMarketplaceItems() {
      axios.get('/api/jdbc/copyright/marketplace-with-usernames')
        .then(response => {
          this.items = response.data
        })
        .catch(error => {
          this.$Message.error('加载拍卖市场数据失败')
          console.error('加载拍卖市场数据失败:', error)
        })
    },
    refreshData() {
      console.log('刷新拍卖市场数据...')
      this.loadMarketplaceItems()
    },
    getTimeRemaining(endTimeStr) {
      if (!endTimeStr) return '未知'
      const endTime = new Date(endTimeStr)
      const now = new Date()
      if (endTime <= now) {
        return '已结束'
      }
      const diffMs = endTime - now
      const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24))
      const diffHrs = Math.floor((diffMs % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))
      const diffMins = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60))
      if (diffDays > 0) {
        return `${diffDays}天 ${diffHrs}小时`
      } else if (diffHrs > 0) {
        return `${diffHrs}小时 ${diffMins}分钟`
      } else {
        return `${diffMins}分钟`
      }
    },
    openItemDetails(item) {
      this.selectedItem = item
      this.bidAmount = this.getMinBidAmount()
      this.detailsModal = true
    },
    getMinBidAmount() {
      if (!this.selectedItem) return 0
      // 最小出价为当前价格加上5%
      return parseFloat((this.selectedItem.price * 1.05).toFixed(2))
    },
    getAttachments(attachmentPaths) {
      if (!attachmentPaths) return []
      return attachmentPaths.split(',').filter(path => path.trim() !== '')
    },
    getAttachmentName(path) {
      const parts = path.split('/')
      return parts[parts.length - 1]
    },
    downloadAttachment(fileName) {
      const downloadUrl = `/api/jdbc/copyright/download/attachment/${fileName}`
      const a = document.createElement('a')
      a.href = downloadUrl
      a.download = this.getAttachmentName(fileName)
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
    },
    async placeBid() {
      if (!this.canPlaceBid) {
        this.$Message.warning('无法出价，请检查价格或登录状态')
        return
      }
      this.bidding = true
      try {
        // 区块链交易
        let blockchainSuccess = false
        if (this.web3 && this.contract) {
          try {
            const accounts = await this.web3.eth.getAccounts()
            const account = accounts[0]
            const bidData = {
              itemId: this.selectedItem.id,
              bidder: account,
              amount: this.bidAmount,
              timestamp: new Date().toISOString()
            }
            const methodNames = Object.keys(this.contract.methods)
            const supportedMethods = ['placeBid', 'bid', 'makeBid']
            const method = supportedMethods.find(m => methodNames.includes(m))
            if (method) {
              const result = await this.contract.methods[method](
                this.selectedItem.id,
                JSON.stringify(bidData)
              ).send({
                from: account,
                value: this.web3.utils.toWei(this.bidAmount.toString(), 'ether')
              })
              console.log('区块链交易哈希:', result.transactionHash)
              blockchainSuccess = true
            }
          } catch (blockchainError) {
            console.error('区块链交易失败:', blockchainError)
          }
        }
        // 后端API调用
        await axios.post(`/api/jdbc/copyright/${this.selectedItem.id}/bid`, null, {
          params: {
            bidAmount: this.bidAmount,
            bidderId: this.currentUser.id || null,
            bidderAddress: this.currentUser.address || null
          }
        })
        this.$Message.success(blockchainSuccess
          ? '出价成功，区块链交易已确认！'
          : '出价成功！')
        // 重新加载数据并关闭弹窗
        this.loadMarketplaceItems()
        this.detailsModal = false
      } catch (error) {
        this.$Message.error('出价失败: ' + (error.response && error.response.data && error.response.data.message ? error.response.data.message : '未知错误'))
        console.error('出价失败:', error)
      } finally {
        this.bidding = false
      }
    }
  }
}
</script>

<style scoped>
.marketplace {
  padding: 20px;
}

.page-title {
  margin-bottom: 24px;
  color: #17233d;
}

.filter-bar {
  display: flex;
  margin-bottom: 20px;
  align-items: center;
}

.items-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  margin-top: 30px;
}

.item-card {
  transition: all 0.3s ease;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.item-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 15px rgba(0,0,0,0.1);
}

.item-image {
  height: 200px;
  overflow: hidden;
  cursor: pointer;
  position: relative;
}

.item-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.item-image:hover img {
  transform: scale(1.05);
}

.item-info {
  padding: 16px;
  display: flex;
  flex-direction: column;
  flex-grow: 1;
}

.item-title {
  margin-bottom: 10px;
  font-size: 16px;
  line-height: 1.4;
  height: 44px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.item-details {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}

.item-price {
  font-weight: bold;
  color: #2d8cf0;
}

.item-time {
  color: #808695;
  font-size: 14px;
}

.item-category {
  margin-bottom: 16px;
}

.no-items {
  grid-column: 1 / -1;
  text-align: center;
  padding: 40px;
  color: #808695;
}

/* 详情弹窗样式 */
.item-details-modal {
  display: flex;
  gap: 24px;
}

.item-details-image {
  flex: 0 0 45%;
}

.item-details-image img {
  width: 100%;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.item-details-info {
  flex: 1;
}

.seller-info {
  margin-top: 8px;
  font-size: 14px;
  color: #515a6e;
}

.price-section, .time-section {
  margin: 16px 0;
}

.current-price {
  font-size: 24px;
  font-weight: bold;
  color: #2d8cf0;
}

.time-remaining {
  font-size: 18px;
  color: #e03131;
}

.description-section, .category-section, .attachments-section {
  margin: 16px 0;
}

.attachment-list {
  list-style: none;
  padding: 0;
}

.attachment-link {
  color: #2d8cf0;
  cursor: pointer;
  display: inline-block;
  margin: 5px 0;
}

.attachment-link:hover {
  text-decoration: underline;
}

.bid-section {
  margin-top: 24px;
  display: flex;
  gap: 12px;
}

@media (max-width: 768px) {
  .item-details-modal {
    flex-direction: column;
  }
  
  .item-details-image {
    flex: none;
  }
}
</style> 
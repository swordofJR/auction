<template>
  <div class="marketplace vm-margin">
    <h1 class="page-title">拍卖市场</h1>
    <div class="filter-bar">
      <Input v-model="searchQuery" placeholder="搜索竞品..." style="width: 200px" />
      <Select v-model="categoryFilter" style="width: 150px; margin-left: 10px;">
        <Option value="">全部类别</Option>
        <Option v-for="(cat, index) in categories" :value="cat" :key="index">{{ cat }}</Option>
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
          <img :src="require('../assets/img/image.png')" alt="竞品图片" />
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
          <img :src="require('../assets/img/image.png')" alt="竞品图片" />
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
            <div class="time-remaining" :class="{'ended': getTimeRemaining(selectedItem.auctionEndTime) === '已结束' || selectedItem.ended}">
              {{ selectedItem.ended ? '已结束' : getTimeRemaining(selectedItem.auctionEndTime) }}
            </div>
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
          <div class="bid-section" v-if="!selectedItem.ended && getTimeRemaining(selectedItem.auctionEndTime) !== '已结束'">
            <InputNumber v-model="bidAmount" :min="getMinBidAmount()" :step="0.01" placeholder="出价金额 (ETH)" style="width: 200px" />
            <Button type="primary" :loading="bidding" :disabled="!canPlaceBid" @click="placeBid">
              {{ bidding ? '处理中...' : '出价' }}
            </Button>
          </div>
          <!-- 拍卖控制按钮 -->
          <div class="auction-controls" v-if="currentUser && (currentUser.id === selectedItem.userId || new Date(selectedItem.auctionEndTime) < new Date())">
            <Button type="warning" :disabled="selectedItem.ended" @click="endAuction">结束拍卖</Button>
          </div>
          <!-- 出价历史按钮 -->
          <div class="bid-history-section">
            <Button type="text" @click="showBidHistory = true">查看出价历史</Button>
          </div>
        </div>
      </div>
      <div slot="footer">
        <Button @click="detailsModal = false">关闭</Button>
      </div>
    </Modal>
    <!-- 出价历史弹窗 -->
    <Modal v-model="showBidHistory" title="出价历史" width="500" :mask-closable="true">
      <div class="bid-history">
        <Table :columns="bidHistoryColumns" :data="bidHistoryData" :loading="loadingBidHistory">
          <template slot="empty">
            <div class="no-bids">
              <p>暂无出价记录</p>
            </div>
          </template>
        </Table>
      </div>
      <div slot="footer">
        <Button @click="showBidHistory = false">关闭</Button>
      </div>
    </Modal>
  </div>
</template>
<script>
import axios from 'axios';
import Web3 from 'web3';
import { abi } from '../contracts/SimpleAuction.json';
import { contractAddress } from '../contracts/config';
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
      refreshInterval: null,
      timeUpdateInterval: null,
      showBidHistory: false,
      bidHistoryColumns: [
        {
          title: '出价人',
          key: 'bidderName',
          width: '30%'
        },
        {
          title: '出价金额',
          key: 'bidAmount',
          width: '30%'
        },
        {
          title: '出价时间',
          key: 'bidTime',
          width: '40%'
        }
      ],
      bidHistoryData: [],
      loadingBidHistory: false
    }
  },
  computed: {
    filteredItems() {
      let result = [...this.items]
      if (this.searchQuery) {
        const query = this.searchQuery.toLowerCase()
        result = result.filter(item =>
          item.title.toLowerCase().includes(query) ||
          item.description.toLowerCase().includes(query)
        )
      }
      if (this.categoryFilter) {
        result = result.filter(item => item.category === this.categoryFilter)
      }
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
      if (this.bidAmount <= this.selectedItem.price) {
        return false
      }
      if (this.selectedItem.ended) {
        return false
      }
      const endTime = new Date(this.selectedItem.auctionEndTime)
      if (endTime < new Date()) {
        return false
      }
      if (this.currentUser.id === this.selectedItem.userId) {
        return false
      }
      return true
    }
  },
  async created() {
    await this.initWeb3()
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
    this.refreshInterval = setInterval(() => {
      this.refreshData()
    }, 30000)
    this.timeUpdateInterval = setInterval(() => {
      this.updateTimeRemaining()
    }, 10000)
  },
  beforeDestroy() {
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval)
    }
    if (this.timeUpdateInterval) {
      clearInterval(this.timeUpdateInterval)
    }
  },
  methods: {
    async initWeb3() {
      if (window.ethereum) {
        try {
          await window.ethereum.request({ method: 'eth_requestAccounts' });
          this.web3 = new Web3(window.ethereum);
          this.contract = new this.web3.eth.Contract(abi, contractAddress);
          if (!this.contract.methods) {
            console.error('合约方法对象不存在，请检查ABI');
            this.$Message.warning('智能合约初始化异常，拍卖功能可能无法正常使用');
            return;
          }
          const requiredMethods = ['placeBid', 'getAuction', 'getActiveAuctions', 'endAuction'];
          const missingMethods = [];
          for (const method of requiredMethods) {
            if (!this.contract.methods[method]) {
              missingMethods.push(method);
            }
          }
          if (missingMethods.length > 0) {
            console.error('合约缺少必要方法:', missingMethods);
            this.$Message.warning(`智能合约缺少必要方法: ${missingMethods.join(', ')}`);
          } else {
            try {
              const count = await this.contract.methods.auctionCount().call();
              console.log('当前拍卖总数:', count);
            } catch (validationError) {
              console.error('合约方法调用失败:', validationError);
            }
          }
        } catch (error) {
          console.error('初始化Web3失败:', error);
          this.$Message.error('区块链连接失败: ' + error.message);
        }
      } else {
        console.warn('未检测到MetaMask插件');
        this.$Message.warning('请安装MetaMask插件以便使用拍卖功能');
      }
    },
    async loadMarketplaceItems() {
      try {
        const response = await axios.get('/api/jdbc/copyright/marketplace-with-usernames');
        const dbItems = response.data;
        this.items = dbItems;
        if (this.web3 && this.contract && this.contract.methods && this.contract.methods.getActiveAuctions) {
          try {
            const activeAuctionIds = await this.contract.methods.getActiveAuctions().call();
            console.log('活跃拍卖ID:', activeAuctionIds);
            if (this.selectedItem && this.selectedItem.id) {
              try {
                const auctionInfo = await this.contract.methods.getAuction(this.selectedItem.id).call();
                if (auctionInfo && !auctionInfo.ended) {
                  const highestBidInEther = this.web3.utils.fromWei(auctionInfo.highestBid, 'ether');
                  this.selectedItem.price = parseFloat(highestBidInEther);
                  this.selectedItem.ended = auctionInfo.ended;
                  if (this.detailsModal) {
                    this.loadBidHistory();
                  }
                }
              } catch (error) {
                console.error('获取选中拍卖详情失败:', error);
              }
            }
          } catch (blockchainError) {
            console.error('从区块链获取拍卖信息失败:', blockchainError);
          }
        }
      } catch (error) {
        this.$Message.error('加载拍卖市场数据失败');
        console.error('加载拍卖市场数据失败:', error);
      }
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
      if (item && item.id) {
        this.loadBidHistory()
      }
    },
    getMinBidAmount() {
      if (!this.selectedItem) return 0
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
      const downloadUrl = `/api/jdbc/copyright/attachments/download/${fileName}`
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
        if (!this.web3 || !window.ethereum) {
          this.$Message.error('区块链连接异常，请安装并登录MetaMask')
          return
        }
        let accounts = []
        try {
          accounts = await window.ethereum.request({ method: 'eth_accounts' })
          if (accounts.length === 0) {
            accounts = await window.ethereum.request({ method: 'eth_requestAccounts' })
          }
        } catch (connError) {
          this.$Message.error('MetaMask连接失败: ' + connError.message)
          return
        }
        const account = accounts[0]
        const bidAmountWei = this.web3.utils.toWei(this.bidAmount.toString(), 'ether')
        const balance = await this.web3.eth.getBalance(account)
        if (parseFloat(balance) < parseFloat(bidAmountWei)) {
          this.$Message.error('账户余额不足，无法完成交易')
          return
        }
        let transactionHash = null
        if (this.contract && this.contract.methods && this.contract.methods.placeBid) {
          try {
            const gasPrice = await this.web3.eth.getGasPrice()
            const result = await this.contract.methods.placeBid(this.selectedItem.id).send({
              from: account,
              value: bidAmountWei,
              gas: 3000000,
              gasPrice: gasPrice
            })
            transactionHash = result.transactionHash
            console.log('区块链交易成功，Hash:', transactionHash)
          } catch (txError) {
            console.warn('区块链交易失败:', txError)
            if (txError.code === 4001) {
              this.$Message.destroy()
              this.$Message.warning('用户取消了交易')
              this.bidding = false
              return
            }
            console.warn('继续使用后端API记录出价')
          }
        }
        this.$Message.destroy()
        try {
          const response = await axios.post(`/api/jdbc/copyright/${this.selectedItem.id}/bid`, null, {
            params: {
              userId: this.currentUser.id,
              bidAmount: this.bidAmount,
              bidderAddress: account,
              transactionHash: transactionHash || 'local-transaction'
            }
          })
          if (response.data) {
            this.$Message.success('出价成功！')
            this.selectedItem.price = this.bidAmount
            this.loadMarketplaceItems()
            this.loadBidHistory()
          } else {
            this.$Message.error('出价失败：价格必须高于当前价格')
          }
        } catch (apiError) {
          let errorMsg = '后端处理出价失败'
          if (apiError.response && apiError.response.data) {
            errorMsg = apiError.response.data
          } else if (apiError.message) {
            errorMsg = apiError.message
          }
          this.$Message.error(errorMsg)
          console.error('后端API调用失败:', apiError)
        }
      } catch (error) {
        let errorMsg = '出价失败'
        if (error.code === 4001) {
          errorMsg = '用户取消了交易'
        } else if (error.message && error.message.includes('insufficient funds')) {
          errorMsg = '账户余额不足'
        } else if (error.response && error.response.data) {
          errorMsg = error.response.data
        } else if (error.message) {
          errorMsg = error.message
        }
        this.$Message.error(errorMsg)
        console.error('出价失败:', error)
      } finally {
        this.bidding = false
      }
    },
    updateTimeRemaining() {
      if (this.detailsModal && this.selectedItem) {
        this.$forceUpdate()
      }
    },
    async endAuction() {
      if (!this.selectedItem || !this.currentUser) {
        this.$Message.warning('无法结束拍卖，请检查登录状态')
        return
      }
      try {
        const endTime = new Date(this.selectedItem.auctionEndTime)
        const now = new Date()
        const isCreator = this.currentUser.id === this.selectedItem.userId
        const isAdmin = this.currentUser.role === 'admin'
        // 管理员可以随时结束拍卖
        if (!isCreator && !isAdmin && endTime > now) {
          this.$Message.warning('只有创建者或管理员可以在结束时间前终止拍卖')
          return
        }
        this.$Message.loading({
          content: '正在结束拍卖...',
          duration: 0
        })
        let highestBidder = null
        let highestBid = this.selectedItem.price
        let highestBidderId = null
        let txHash = null
        try {
          const bidHistoryResponse = await axios.get(`/api/jdbc/copyright/${this.selectedItem.id}/bid-history`)
          if (bidHistoryResponse.data && bidHistoryResponse.data.length > 0) {
            const highestBidInfo = bidHistoryResponse.data[0]
            highestBidder = highestBidInfo.bidderAddress
            highestBid = highestBidInfo.bidAmount
            highestBidderId = highestBidInfo.bidderId
          }
        } catch (bidError) {
          console.warn('获取出价历史失败，使用默认数据', bidError)
        }
        if (this.web3 && this.contract && this.contract.methods.endAuction) {
          try {
            const accounts = await this.web3.eth.getAccounts()
            if (accounts && accounts.length > 0) {
              const account = accounts[0]
              try {
                const auctionInfo = await this.contract.methods.getAuction(this.selectedItem.id).call()
                if (auctionInfo) {
                  highestBidder = auctionInfo.highestBidder !== '0x0000000000000000000000000000000000000000'
                    ? auctionInfo.highestBidder : account
                  highestBid = this.web3.utils.fromWei(auctionInfo.highestBid, 'ether')
                }
              } catch (blockchainError) {
                console.warn('获取区块链拍卖信息失败，使用本地数据', blockchainError)
              }
              try {
                const result = await this.contract.methods.endAuction(this.selectedItem.id).send({
                  from: account,
                  gas: 300000
                })
                if (result && result.transactionHash) {
                  txHash = result.transactionHash
                  console.log('区块链交易成功，Hash:', txHash)
                }
              } catch (txError) {
                if (txError.code === 4001) {
                  this.$Message.destroy()
                  this.$Message.warning('用户取消了交易')
                  return
                }
                console.warn('区块链交易失败，继续使用后端API更新状态', txError)
              }
            }
          } catch (web3Error) {
            console.warn('Web3交互失败，继续使用后端API更新状态', web3Error)
          }
        }
        try {
          // 直接创建请求参数对象
          const params = new URLSearchParams()
          params.append('newOwnerAddress', highestBidder || 'unknown')
          params.append('finalPrice', highestBid)
          params.append('transactionHash', txHash || 'backend-generated')
          params.append('newOwnerId', highestBidderId || '')
          params.append('forceEnd', 'true')
          // 调用API结束拍卖
          const response = await axios.post(`/api/jdbc/copyright/${this.selectedItem.id}/delist`, params, {
            headers: {
              'Content-Type': 'application/x-www-form-urlencoded'
            }
          });
          // 记录日志便于调试
          console.log('结束拍卖API响应:', response.data);
          this.$Message.destroy()
          this.$Message.success('拍卖已成功结束！物品已转移给最高出价者')
          // 更新UI状态
          this.selectedItem.ended = true
          this.selectedItem.status = 'SOLD'
          this.selectedItem.ownerAddress = highestBidder || 'unknown'
          this.selectedItem.userId = highestBidderId
          this.items = this.items.filter(item => item.id !== this.selectedItem.id)
          this.detailsModal = false
          if (this.currentUser.id === highestBidderId) {
            this.$Message.info('恭喜！您已成功拍得该物品，可在"我的竞品"中查看')
          }
        } catch (apiError) {
          console.error('结束拍卖API调用失败:', apiError);
          this.$Message.destroy()
          let errorMsg = '结束拍卖失败'
          if (apiError.response && apiError.response.data) {
            if (typeof apiError.response.data === 'string') {
              errorMsg = apiError.response.data
            } else if (apiError.response.data.message) {
              errorMsg = apiError.response.data.message
            }
            console.error('服务器响应错误:', apiError.response.status, apiError.response.data);
          } else if (apiError.message) {
            errorMsg = apiError.message
          }
          this.$Message.error(errorMsg)
        }
      } catch (error) {
        this.$Message.destroy()
        let errorMsg = '结束拍卖失败'
        if (error.code === 4001) {
          errorMsg = '用户取消了交易'
        } else if (error.response && error.response.data) {
          if (typeof error.response.data === 'string') {
            errorMsg = error.response.data
          } else if (error.response.data.message) {
            errorMsg = error.response.data.message
          } else {
            errorMsg = `服务器错误(${error.response.status})`
          }
          console.error('服务器响应错误:', error.response.status, error.response.data);
        } else if (error.message) {
          errorMsg = error.message
        }
        this.$Message.error(errorMsg)
        console.error('结束拍卖失败:', error)
      }
    },
    async loadBidHistory() {
      if (!this.selectedItem || !this.selectedItem.id) return;
      try {
        this.loadingBidHistory = true;
        const response = await axios.get(`/api/jdbc/copyright/${this.selectedItem.id}/bid-history`);
        this.bidHistoryData = response.data && response.data.length > 0 ? response.data : [{
          bidderName: this.selectedItem.username || '当前最高出价者',
          bidAmount: this.selectedItem.price,
          bidTime: new Date().toLocaleString()
        }];
        if (this.web3 && this.contract && this.contract.methods.getBidCount) {
          try {
            const bidCount = await this.contract.methods.getBidCount(this.selectedItem.id).call();
            console.log(`竞品ID ${this.selectedItem.id} 总共有 ${bidCount} 个出价记录`);
          } catch (blockchainError) {
            console.log('从区块链获取出价记录失败:', blockchainError);
          }
        }
      } catch (error) {
        // console.error('加载出价历史失败:', error);
      } finally {
        this.loadingBidHistory = false;
      }
    },
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
.time-remaining.ended {
  color: #808695;
  font-style: italic;
}
.no-bids {
  text-align: center;
  padding: 20px;
  color: #808695;
}
.bid-history {
  max-height: 400px;
  overflow-y: auto;
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
.auction-controls {
  margin-top: 24px;
  display: flex;
  gap: 12px;
}
.bid-history-section {
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
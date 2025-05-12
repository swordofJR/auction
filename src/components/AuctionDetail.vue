<template>
  <div class="auction-detail">
    <div class="auction-header">
      <h2>竞品详情</h2>
      <div class="auction-status" :class="{ 'active': isActive, 'ended': isEnded }">
        {{ auctionStatusText }}
      </div>
    </div>
    
    <div class="auction-info">
      <div class="auction-item">
        <img :src="copyrightImage" alt="版权图片" class="copyright-image" />
        <div class="auction-item-info">
          <h3>{{ copyrightTitle }}</h3>
          <p class="description">{{ copyrightDescription }}</p>
          
          <div class="auction-details">
            <div class="detail-item">
              <span class="label">版权所有者:</span>
              <span class="value">{{ shortAddress(auction.seller) }}</span>
            </div>
            <div class="detail-item">
              <span class="label">起拍价:</span>
              <span class="value">{{ formatEther(auction.startingPrice) }} ETH</span>
            </div>
            <div class="detail-item">
              <span class="label">当前最高出价:</span>
              <span class="value">{{ formatEther(auction.highestBid) }} ETH</span>
            </div>
            <div class="detail-item">
              <span class="label">最高出价者:</span>
              <span class="value">{{ auction.highestBidder ? shortAddress(auction.highestBidder) : '暂无' }}</span>
            </div>
            <div class="detail-item">
              <span class="label">拍卖结束时间:</span>
              <span class="value">{{ formatEndTime }}</span>
            </div>
            <div class="detail-item">
              <span class="label">剩余时间:</span>
              <span class="value countdown">{{ remainingTime }}</span>
            </div>
          </div>
        </div>
      </div>
      
      <div class="bid-section" v-if="isActive && !isEnded">
        <h3>出价</h3>
        <div class="bid-input">
          <input 
            type="number" 
            v-model="bidAmount" 
            :min="minBidAmount" 
            step="0.01" 
            placeholder="输入出价金额(ETH)"
          />
          <button 
            @click="placeBid" 
            :disabled="!isValidBid || isLoading"
            class="bid-button"
          >
            {{ isLoading ? '处理中...' : '出价' }}
          </button>
        </div>
        <p class="min-bid">最低出价: {{ formatEther(minBidAmount) }} ETH</p>
        <div class="metamask-status">
          <span v-if="!isMetaMaskConnected" class="warning">
            MetaMask 未连接，请先连接钱包
            <button @click="connectMetaMask" class="connect-button">连接 MetaMask</button>
          </span>
          <span v-else class="connected">
            钱包已连接: {{ shortAddress(currentAccount) }}
          </span>
        </div>
      </div>
      
      <div class="action-buttons" v-if="isOwner && !isEnded && !auction.highestBidder">
        <button @click="cancelAuction" :disabled="isLoading" class="cancel-button">
          取消拍卖
        </button>
      </div>
      
      <div class="action-buttons" v-if="(isOwner || isAdmin) && isEnded && !auction.ended">
        <button @click="endAuction" :disabled="isLoading" class="end-button">
          结束拍卖
        </button>
      </div>
      
      <div class="auction-result" v-if="auction.ended">
        <h3>拍卖结果</h3>
        <div v-if="auction.highestBidder" class="success-result">
          <p>恭喜! 拍卖成功结束</p>
          <p>获胜者: {{ shortAddress(auction.highestBidder) }}</p>
          <p>最终成交价: {{ formatEther(auction.highestBid) }} ETH</p>
        </div>
        <div v-else class="failed-result">
          <p>拍卖结束，无人出价</p>
          <p>版权已返还给原所有者</p>
        </div>
      </div>
    </div>
    
    <div class="notification" v-if="notification.show">
      <div class="notification-content" :class="notification.type">
        {{ notification.message }}
      </div>
    </div>
  </div>
</template>

<script>
import { ethers } from 'ethers';
import AuctionMarketplace from '../../build/contracts/AuctionMarketplace.json';
import { ref, computed, onMounted, onUnmounted } from 'vue';

export default {
  name: 'AuctionDetail',
  props: {
    tokenId: {
      type: [String, Number],
      required: true
    },
    copyrightTitle: {
      type: String,
      default: '版权作品'
    },
    copyrightDescription: {
      type: String,
      default: '这是一个版权作品的描述'
    },
    copyrightImage: {
      type: String,
      default: '/static/default-copyright.jpg'
    },
    isAdmin: {
      type: Boolean,
      default: false
    }
  },
  setup(props) {
    // 拍卖数据
    const auction = ref({
      seller: '',
      startingPrice: ethers.utils.parseEther('0'),
      highestBid: ethers.utils.parseEther('0'),
      highestBidder: null,
      endTime: 0,
      isActive: false,
      ended: false
    });
    
    // 用户状态
    const currentAccount = ref('');
    const isMetaMaskConnected = ref(false);
    const isLoading = ref(false);
    
    // 出价状态
    const bidAmount = ref('');
    const minBidAmount = computed(() => {
      // 如果有最高出价，新出价应该比最高出价高
      if (auction.value.highestBid && ethers.BigNumber.from(auction.value.highestBid).gt(0)) {
        return ethers.BigNumber.from(auction.value.highestBid).add(
          ethers.utils.parseEther('0.01')
        );
      }
      // 否则，至少等于起拍价
      return auction.value.startingPrice;
    });
    
    // 判断是否是有效出价
    const isValidBid = computed(() => {
      // 必须先连接MetaMask
      if (!isMetaMaskConnected.value) return false;
      
      // 必须输入金额
      if (!bidAmount.value || bidAmount.value <= 0) return false;
      
      try {
        // 转换为Wei单位
        const bidAmountInWei = ethers.utils.parseEther(bidAmount.value.toString());
        
        // 确保出价高于最低出价
        return bidAmountInWei.gt(minBidAmount.value);
      } catch (e) {
        console.error('解析出价金额失败:', e);
        return false;
      }
    });
    
    // 计算拍卖状态
    const isActive = computed(() => {
      return auction.value.isActive && !auction.value.ended;
    });
    
    const isEnded = computed(() => {
      return auction.value.ended || auction.value.endTime <= Date.now() / 1000;
    });
    
    const auctionStatusText = computed(() => {
      if (auction.value.ended) return '拍卖已结束';
      if (!auction.value.isActive) return '拍卖未激活';
      if (auction.value.endTime <= Date.now() / 1000) return '拍卖已超时';
      return '拍卖进行中';
    });
    
    // 判断当前用户是否是拍卖创建者
    const isOwner = computed(() => {
      return currentAccount.value.toLowerCase() === auction.value.seller.toLowerCase();
    });
    
    // 格式化时间显示
    const formatEndTime = computed(() => {
      if (!auction.value.endTime) return '未设置';
      
      const endDate = new Date(auction.value.endTime * 1000);
      return endDate.toLocaleString();
    });
    
    // 剩余时间计算
    const remainingTime = ref('计算中...');
    let timer = null;
    
    const updateRemainingTime = () => {
      if (!auction.value.endTime) {
        remainingTime.value = '未设置';
        return;
      }
      
      const now = Math.floor(Date.now() / 1000);
      const remaining = auction.value.endTime - now;
      
      if (remaining <= 0) {
        remainingTime.value = '已结束';
        return;
      }
      
      const days = Math.floor(remaining / 86400);
      const hours = Math.floor((remaining % 86400) / 3600);
      const minutes = Math.floor((remaining % 3600) / 60);
      const seconds = remaining % 60;
      
      remainingTime.value = `${days}天 ${hours}小时 ${minutes}分钟 ${seconds}秒`;
    };
    
    // 通知消息
    const notification = ref({
      show: false,
      message: '',
      type: 'info'
    });
    
    const showNotification = (message, type = 'info') => {
      notification.value = {
        show: true,
        message,
        type
      };
      
      setTimeout(() => {
        notification.value.show = false;
      }, 5000);
    };
    
    // MetaMask 连接
    const connectMetaMask = async () => {
      if (typeof window.ethereum === 'undefined') {
        showNotification('请安装 MetaMask 插件', 'error');
        return;
      }
      
      try {
        isLoading.value = true;
        const accounts = await window.ethereum.request({ method: 'eth_requestAccounts' });
        currentAccount.value = accounts[0];
        isMetaMaskConnected.value = true;
        showNotification('MetaMask 连接成功', 'success');
      } catch (error) {
        console.error('MetaMask 连接失败:', error);
        showNotification('MetaMask 连接失败: ' + error.message, 'error');
      } finally {
        isLoading.value = false;
      }
    };
    
    // 链接变化监听
    const handleAccountsChanged = (accounts) => {
      if (accounts.length === 0) {
        isMetaMaskConnected.value = false;
        currentAccount.value = '';
        showNotification('MetaMask 已断开连接', 'warning');
      } else if (accounts[0] !== currentAccount.value) {
        currentAccount.value = accounts[0];
        isMetaMaskConnected.value = true;
        showNotification('已切换 MetaMask 账户', 'info');
      }
    };
    
    // 获取合约实例
    const getAuctionContract = async () => {
      try {
        const provider = new ethers.providers.Web3Provider(window.ethereum);
        const signer = provider.getSigner();
        const networkId = (await provider.getNetwork()).chainId;
        
        const deployedNetwork = AuctionMarketplace.networks[networkId];
        if (!deployedNetwork) {
          showNotification('合约未部署到当前网络', 'error');
          return null;
        }
        
        return new ethers.Contract(
          deployedNetwork.address,
          AuctionMarketplace.abi,
          signer
        );
      } catch (error) {
        console.error('获取合约失败:', error);
        showNotification('获取合约失败: ' + error.message, 'error');
        return null;
      }
    };
    
    // 加载拍卖数据
    const loadAuctionData = async () => {
      try {
        isLoading.value = true;
        const contract = await getAuctionContract();
        if (!contract) return;
        
        const auctionData = await contract.getAuction(props.tokenId);
        
        auction.value = {
          seller: auctionData.seller,
          startingPrice: auctionData.startingPrice,
          highestBid: auctionData.highestBid,
          highestBidder: auctionData.highestBidder,
          endTime: auctionData.endTime.toNumber(),
          isActive: auctionData.isActive,
          ended: auctionData.ended
        };
        
        updateRemainingTime();
      } catch (error) {
        console.error('加载拍卖数据失败:', error);
        showNotification('加载拍卖数据失败: ' + error.message, 'error');
      } finally {
        isLoading.value = false;
      }
    };
    
    // 出价
    const placeBid = async () => {
      if (!isMetaMaskConnected.value) {
        showNotification('请先连接MetaMask钱包', 'warning');
        return;
      }
      
      if (!isValidBid.value) {
        showNotification('请输入有效的出价金额', 'error');
        return;
      }
      
      try {
        isLoading.value = true;
        const contract = await getAuctionContract();
        if (!contract) return;
        
        // 确保转换为字符串后再解析为ETH金额
        const bidAmountInWei = ethers.utils.parseEther(bidAmount.value.toString());
        
        // 发送交易
        const tx = await contract.placeBid(props.tokenId, {
          value: bidAmountInWei
        });
        
        showNotification('出价交易已提交，等待确认...', 'info');
        
        // 等待交易确认
        await tx.wait();
        
        showNotification('出价成功!', 'success');
        bidAmount.value = '';
        
        // 重新加载拍卖数据
        await loadAuctionData();
      } catch (error) {
        console.error('出价失败:', error);
        const errorMsg = (error.data && error.data.message) || error.message || '未知错误';
        showNotification('出价失败: ' + errorMsg, 'error');
      } finally {
        isLoading.value = false;
      }
    };
    
    // 取消拍卖
    const cancelAuction = async () => {
      if (!isOwner.value && !props.isAdmin) {
        showNotification('只有卖家或管理员可以取消拍卖', 'error');
        return;
      }
      
      try {
        isLoading.value = true;
        const contract = await getAuctionContract();
        if (!contract) return;
        
        const tx = await contract.cancelAuction(props.tokenId);
        
        showNotification('取消拍卖交易已提交，等待确认...', 'info');
        
        await tx.wait();
        
        showNotification('取消拍卖成功!', 'success');
        
        // 重新加载拍卖数据
        await loadAuctionData();
      } catch (error) {
        console.error('取消拍卖失败:', error);
        showNotification('取消拍卖失败: ' + error.message, 'error');
      } finally {
        isLoading.value = false;
      }
    };
    
    // 结束拍卖
    const endAuction = async () => {
      if (!isOwner.value && !props.isAdmin) {
        showNotification('只有卖家或管理员可以结束拍卖', 'error');
        return;
      }
      
      try {
        isLoading.value = true;
        const contract = await getAuctionContract();
        if (!contract) return;
        
        const tx = await contract.endAuction(props.tokenId);
        
        showNotification('结束拍卖交易已提交，等待确认...', 'info');
        
        await tx.wait();
        
        showNotification('拍卖已成功结束!', 'success');
        
        // 重新加载拍卖数据
        await loadAuctionData();
      } catch (error) {
        console.error('结束拍卖失败:', error);
        showNotification('结束拍卖失败: ' + error.message, 'error');
      } finally {
        isLoading.value = false;
      }
    };
    
    // 辅助函数
    const shortAddress = (address) => {
      if (!address) return '无地址';
      return `${address.substring(0, 6)}...${address.substring(address.length - 4)}`;
    };
    
    const formatEther = (value) => {
      if (!value) return '0';
      try {
        return parseFloat(ethers.utils.formatEther(value)).toFixed(4);
      } catch (e) {
        return '0';
      }
    };
    
    // 生命周期钩子
    onMounted(async () => {
      // 检查是否安装 MetaMask
      if (typeof window.ethereum !== 'undefined') {
        // 监听账户变化
        window.ethereum.on('accountsChanged', handleAccountsChanged);
        
        // 检查是否已连接
        try {
          const accounts = await window.ethereum.request({ method: 'eth_accounts' });
          if (accounts.length > 0) {
            currentAccount.value = accounts[0];
            isMetaMaskConnected.value = true;
          }
        } catch (e) {
          console.error('检查 MetaMask 连接状态失败:', e);
        }
      }
      
      // 加载拍卖数据
      await loadAuctionData();
      
      // 启动剩余时间更新计时器
      timer = setInterval(updateRemainingTime, 1000);
    });
    
    onUnmounted(() => {
      // 清除计时器
      if (timer) clearInterval(timer);
      
      // 移除事件监听
      if (typeof window.ethereum !== 'undefined') {
        window.ethereum.removeListener('accountsChanged', handleAccountsChanged);
      }
    });
    
    return {
      auction,
      currentAccount,
      isMetaMaskConnected,
      isLoading,
      bidAmount,
      minBidAmount,
      isValidBid,
      isActive,
      isEnded,
      auctionStatusText,
      isOwner,
      formatEndTime,
      remainingTime,
      notification,
      connectMetaMask,
      placeBid,
      cancelAuction,
      endAuction,
      shortAddress,
      formatEther
    };
  }
};
</script>

<style scoped>
.auction-detail {
  margin: 20px;
  padding: 20px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.auction-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}

.auction-status {
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: bold;
}

.auction-status.active {
  background-color: #e6f7ff;
  color: #1890ff;
}

.auction-status.ended {
  background-color: #f5f5f5;
  color: #999;
}

.auction-item {
  display: flex;
  margin-bottom: 30px;
}

.copyright-image {
  width: 200px;
  height: 200px;
  object-fit: cover;
  border-radius: 8px;
  margin-right: 20px;
}

.auction-item-info {
  flex: 1;
}

.description {
  color: #666;
  margin-bottom: 20px;
}

.auction-details {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 15px;
}

.detail-item {
  display: flex;
  flex-direction: column;
}

.label {
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
}

.value {
  font-size: 16px;
  font-weight: 500;
}

.countdown {
  color: #ff4d4f;
  font-weight: bold;
}

.bid-section {
  background-color: #f9f9f9;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.bid-input {
  display: flex;
  margin-bottom: 10px;
}

.bid-input input {
  flex: 1;
  padding: 10px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  margin-right: 10px;
}

.bid-button {
  padding: 10px 20px;
  background-color: #1890ff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.bid-button:hover {
  background-color: #40a9ff;
}

.bid-button:disabled {
  background-color: #d9d9d9;
  cursor: not-allowed;
}

.min-bid {
  font-size: 12px;
  color: #666;
  margin-bottom: 10px;
}

.metamask-status {
  margin-top: 10px;
}

.warning {
  color: #faad14;
  display: flex;
  align-items: center;
}

.connect-button {
  margin-left: 10px;
  padding: 5px 10px;
  background-color: #faad14;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.connected {
  color: #52c41a;
}

.action-buttons {
  margin-top: 20px;
}

.cancel-button, .end-button {
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.cancel-button {
  background-color: #ff4d4f;
  color: white;
}

.cancel-button:hover {
  background-color: #ff7875;
}

.end-button {
  background-color: #52c41a;
  color: white;
}

.end-button:hover {
  background-color: #73d13d;
}

.auction-result {
  margin-top: 30px;
  padding: 20px;
  border-radius: 8px;
}

.success-result {
  background-color: #f6ffed;
  border: 1px solid #b7eb8f;
}

.failed-result {
  background-color: #fff2e8;
  border: 1px solid #ffccc7;
}

.notification {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 1000;
}

.notification-content {
  padding: 10px 20px;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  margin-bottom: 10px;
  animation: fadeIn 0.3s;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}

.notification-content.info {
  background-color: #e6f7ff;
  border: 1px solid #91d5ff;
}

.notification-content.success {
  background-color: #f6ffed;
  border: 1px solid #b7eb8f;
}

.notification-content.warning {
  background-color: #fffbe6;
  border: 1px solid #ffe58f;
}

.notification-content.error {
  background-color: #fff2f0;
  border: 1px solid #ffccc7;
}
</style> 
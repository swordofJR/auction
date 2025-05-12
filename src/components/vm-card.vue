<template>
  <div :class="[type === 'horizantal' ? 'vm-card-horizantal' : 'vm-card-vertical' , 'vm-panel']">
    <div class="card-img">
      <img :src="img" alt="">
      <div v-if="editable && type == 'vertical'" class="control">
        <span class="edit">
          <i class="fa fa-shopping-bag" @click="openBuyModal"></i>
        </span>
      </div>
    </div>
    <div class="card-desc panel-body">
      <h2>{{ title }}</h2>
      <p>{{ desc }}</p>
      <div class="card-price" v-if="price">
        <span class="price-label">价格：</span>
        <span class="price-value">{{ price }} ETH</span>
      </div>
      <div class="card-owner" v-if="username">
        <span class="owner-label">所有者：</span>
        <span class="owner-value">{{ username }}</span>
      </div>
      <a :href="detailUrl">
        <!-- more > -->
      </a>
      <div class="card-actions" v-if="isAdmin && status === 'LISTED'">
        <Button type="error" size="small" @click="handleDelist">下架商品</Button>
      </div>
    </div>

    <!-- 购买确认弹窗 -->
    <Modal
      v-model="showBuyModal"
      title="确认购买"
      @on-ok="handleBuy"
      @on-cancel="cancelBuy">
      <div class="buy-modal-content">
        <div class="modal-item">
          <span class="label">藏品名称：</span>
          <span class="value">{{ title }}</span>
        </div>
        <div class="modal-item">
          <span class="label">拥有者：</span>
          <span class="value">{{ username || '未知用户' }}</span>
        </div>
        <div class="modal-item">
          <span class="label">钱包地址：</span>
          <span class="value">{{ ownerAddress || 'Unknown' }}</span>
        </div>
        <div class="modal-item">
          <span class="label">描述：</span>
          <span class="value">{{ desc }}</span>
        </div>
        <div class="modal-item">
          <span class="label">类别：</span>
          <span class="value">{{ category || 'Unknown' }}</span>
        </div>
        <div class="modal-item">
          <span class="label">价格：</span>
          <span class="value price-value">{{ price }} ETH</span>
        </div>
        <div class="modal-item">
          <span class="label">状态：</span>
          <span class="value status-available">{{ status || '可交易' }}</span>
        </div>
      </div>
    </Modal>

    <div v-if="attachmentPaths" class="card-attachments">
      <p class="attachment-title">附件文件:</p>
      <ul class="attachment-list">
        <li v-for="(attachment, index) in attachmentsArray" :key="index">
          <a @click="downloadAttachment(attachment)" class="attachment-link">
            <Icon type="ios-paper" /> {{ getAttachmentName(attachment) }}
          </a>
        </li>
      </ul>
    </div>
  </div>
</template>
<script>
  export default {
    name: 'VmCard',
    props: {
      type: {
        type: String,
        default: 'vertical'
      },
      editable: {
        type: Boolean,
        default: false
      },
      id: {
        type: [Number, String],
        default: ''
      },
      title: {
        type: String,
        default: 'Title'
      },
      img: {
        type: String,
        default: require('@/assets/img/img-1.jpg')
      },
      desc: {
        type: String,
        default: 'Lorem Ipsum is simply dummy text of the printing and typesetting industry,Lorem Ipsum has been the industry\'s standard dummy text ever since the 1500s'
      },
      detailUrl: {
        type: String,
        default: '#'
      },
      editUrl: {
        type: String,
        default: '#'
      },
      price: {
        type: [Number, String],
        default: null
      },
      ownerAddress: {
        type: String,
        default: ''
      },
      category: {
        type: String,
        default: ''
      },
      status: {
        type: String,
        default: 'LISTED'
      },
      isAdmin: {
        type: Boolean,
        default: false
      },
      attachmentPaths: {
        type: String,
        default: ''
      }
    },
    data: function () {
      return {
        showBuyModal: false
      }
    },
    computed: {
      attachmentsArray() {
        if (!this.attachmentPaths) return [];
        return this.attachmentPaths.split(',').filter(path => path.trim() !== '');
      }
    },
    methods: {
      openBuyModal() {
        this.showBuyModal = true;
      },
      handleBuy() {
        this.$emit('edit', {
          title: this.title,
          desc: this.desc,
          price: this.price,
          ownerAddress: this.ownerAddress,
          category: this.category,
          status: this.status,
          username: this.username
        });
        this.showBuyModal = false;
      },
      cancelBuy() {
        this.showBuyModal = false;
      },
      handleDelist() {
        this.$emit('delist', {
          id: this.id,
          title: this.title
        });
      },
      getAttachmentName(path) {
        const parts = path.split('/');
        return parts[parts.length - 1];
      },
      downloadAttachment(fileName) {
        const downloadUrl = `/api/jdbc/copyright/download/attachment/${fileName}`;
        const a = document.createElement('a');
        a.href = downloadUrl;
        a.download = this.getAttachmentName(fileName);
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
      }
    }
  }
</script>

<style lang="less" scoped>
.buy-modal-content {
  .modal-item {
    margin-bottom: 15px;
    display: flex;
    align-items: flex-start;
    
    .label {
      width: 80px;
      color: #666;
      font-weight: bold;
    }
    
    .value {
      flex: 1;
      color: #333;
    }
    
    .status-available {
      color: #19be6b;
    }
  }
}

.card-price {
  margin-top: 12px;
  font-weight: bold;
  
  .price-label {
    color: #666;
  }
  
  .price-value {
    color: #ff9900;
    font-size: 16px;
  }
}

.card-owner {
  margin-top: 8px;
  
  .owner-label {
    color: #666;
    font-weight: bold;
  }
  
  .owner-value {
    color: #2d8cf0;
  }
}

.card-actions {
  margin-top: 12px;
  text-align: right;
}

.card-attachments {
  margin-top: 10px;
  border-top: 1px solid #eee;
  padding-top: 10px;
}

.attachment-title {
  font-weight: bold;
  font-size: 14px;
  margin-bottom: 5px;
  color: #666;
}

.attachment-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.attachment-link {
  color: #2d8cf0;
  cursor: pointer;
  display: inline-block;
  margin-bottom: 5px;
}

.attachment-link:hover {
  text-decoration: underline;
}
</style>

<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, ArrowLeft } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const baseId = route.query.baseId
const searchQuery = ref('')
const searchResults = ref([])
const searching = ref(false)
const hasSearched = ref(false)

// 执行检索
const handleSearch = async () => {
  if (!searchQuery.value.trim()) {
    ElMessage.warning('请输入检索内容')
    return
  }
  
  searching.value = true
  hasSearched.value = true
  
  try {
    const response = await fetch('/api/v0/knowledge/search', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        query: searchQuery.value,
        knowledgeBaseId: baseId,
        maxResults: 20
      })
    })
    
    if (response.ok) {
      const data = await response.json()
      if (data.success) {
        searchResults.value = data.results || []
        ElMessage.success(`找到 ${searchResults.value.length} 条相关结果`)
      } else {
        ElMessage.error(data.message || '检索失败')
        searchResults.value = []
      }
    } else {
      ElMessage.error('检索失败')
      searchResults.value = []
    }
  } catch (e) {
    console.error('检索异常:', e)
    ElMessage.error('检索失败')
    searchResults.value = []
  } finally {
    searching.value = false
  }
}

const handleBack = () => {
  if (baseId) {
    router.push({ path: '/knowledge', query: { baseId } })
  } else {
    router.push('/knowledge')
  }
}

const highlightText = (text, query) => {
  if (!text || !query) return text
  const regex = new RegExp(`(${query})`, 'gi')
  return text.replace(regex, '<mark class="highlight">$1</mark>')
}

const goToChunk = (result) => {
  if (result.docId) {
    router.push({
      path: `/knowledge/chunks/${result.docId}`,
      query: { baseId: baseId }
    })
  }
}
</script>

<template>
  <div class="search-page">
    <div class="page-header">
      <div class="header-left">
        <el-button :icon="ArrowLeft" text @click="handleBack" class="back-btn">
          <template #icon>
            <svg viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round">
              <path d="M19 12H5M12 19l-7-7 7-7"/>
            </svg>
          </template>
        </el-button>
        <h2>知识检索</h2>
      </div>
    </div>

    <div class="page-content">
      <div class="search-box">
        <div class="search-input-wrapper">
          <el-input
            v-model="searchQuery"
            placeholder="请输入要检索的内容..."
            size="large"
            clearable
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
            <template #append>
              <el-button type="primary" @click="handleSearch" :loading="searching">
                检索
              </el-button>
            </template>
          </el-input>
        </div>
      </div>

      <div class="search-results" v-loading="searching">
        <div v-if="!hasSearched" class="search-empty">
          <el-empty description="输入内容开始检索" />
        </div>

        <div v-else-if="searchResults.length === 0" class="search-empty">
          <el-empty description="未找到相关结果" />
        </div>

        <div v-else class="results-list">
          <div class="results-count">找到 {{ searchResults.length }} 条相关结果</div>
          
          <div
            v-for="(result, index) in searchResults"
            :key="result.id || index"
            class="result-item"
            @click="goToChunk(result)"
          >
            <div class="result-header">
              <div class="result-title">
                <el-icon class="title-icon"><Search /></el-icon>
                <span>{{ result.title || '未知文档' }}</span>
              </div>
              <div class="result-score">
                相关度：{{ (result.score * 100).toFixed(1) }}%
              </div>
            </div>
            
            <div class="result-content">
              {{ result.content }}
            </div>
            
            <div class="result-footer">
              <span class="result-chunk">
                切片 {{ (result.chunkIndex || 0) + 1 }}
              </span>
              <span class="result-kb">
                知识库：{{ result.knowledgeBaseId }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.search-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;

  .page-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 20px;
    background-color: #ffffff;
    border-bottom: 1px solid #e4e7ed;
    flex-shrink: 0;

    .header-left {
      display: flex;
      align-items: center;
      gap: 12px;

      .back-btn {
        padding: 6px 8px;
        border-radius: 4px;
        
        &:hover {
          background-color: #f5f7fa;
        }

        :deep(svg) {
          color: #606266;
        }
      }

      h2 {
        margin: 0;
        font-size: 17px;
        font-weight: 600;
        color: #1c1f23;
      }
    }
  }

  .page-content {
    flex: 1;
    padding: 20px;
    overflow-y: auto;

    .search-box {
      margin-bottom: 20px;

      .search-input-wrapper {
        max-width: 800px;
        margin: 0 auto;

        :deep(.el-input) {
          .el-input__wrapper {
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
            border-radius: 8px;
          }
          
          .el-input-group__append {
            border-radius: 0 8px 8px 0;
          }
        }
      }
    }

    .search-results {
      max-width: 800px;
      margin: 0 auto;

      .search-empty {
        height: 300px;
        display: flex;
        align-items: center;
        justify-content: center;
        
        :deep(.el-empty__description) {
          color: #909399;
        }
      }

      .results-list {
        .results-count {
          font-size: 14px;
          color: #606266;
          margin-bottom: 16px;
          padding: 0 8px;
        }

        .result-item {
          background-color: #ffffff;
          border-radius: 8px;
          padding: 16px 20px;
          margin-bottom: 12px;
          border: 1px solid #e4e7ed;
          cursor: pointer;
          transition: all 0.2s ease;

          &:hover {
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
            border-color: #dcdfe6;
            transform: translateY(-1px);
          }

          .result-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 12px;

            .result-title {
              display: flex;
              align-items: center;
              gap: 8px;
              font-size: 16px;
              font-weight: 500;
              color: #303133;

              .title-icon {
                color: #409eff;
                font-size: 18px;
              }
            }

            .result-score {
              font-size: 13px;
              color: #67c23a;
              font-weight: 500;
              background-color: #f0f9eb;
              padding: 4px 8px;
              border-radius: 4px;
            }
          }

          .result-content {
            font-size: 14px;
            line-height: 1.8;
            color: #606266;
            margin-bottom: 12px;
            display: -webkit-box;
            -webkit-line-clamp: 3;
            -webkit-box-orient: vertical;
            overflow: hidden;

            :deep(.highlight) {
              background-color: #ffeb3b;
              padding: 2px 4px;
              border-radius: 2px;
            }
          }

          .result-footer {
            display: flex;
            gap: 16px;
            font-size: 12px;
            color: #909399;

            .result-chunk,
            .result-kb {
              display: flex;
              align-items: center;
              gap: 4px;
            }
          }
        }
      }
    }
  }
}
</style>

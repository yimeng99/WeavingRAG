<template>
  <div class="ai-config">
    <div class="config-header">
      <h2>AI 模型配置</h2>
    </div>

    <div class="config-body">
      <!-- 服务商 -->
      <div class="config-section">
        <div class="section-title">
          <span>服务商</span>
          <el-button type="primary" size="small" @click="handleAddProvider">+ 添加</el-button>
        </div>
        <div class="card-list">
          <div
            v-for="item in providers"
            :key="item.id"
            class="card"
            :class="{ active: currentProviderId === item.id }"
            @click="handleSelectProvider(item)"
          >
            <div class="card-content">
              <div class="card-icon" :style="{ background: getColor(item.providerCode) }">
                {{ item.providerName.charAt(0).toUpperCase() }}
              </div>
              <div class="card-info">
                <div class="card-name">{{ item.providerName }}</div>
                <div class="card-code">{{ item.providerCode }}</div>
              </div>
            </div>
            <div class="card-actions">
              <el-tag size="small" :type="item.status === 1 ? 'success' : 'danger'">
                {{ item.status === 1 ? '启用' : '禁用' }}
              </el-tag>
              <el-button size="small" type="primary" link @click.stop="handleEditProvider(item)">编辑</el-button>
              <el-button size="small" type="danger" link @click.stop="handleDeleteProvider(item.id)">删除</el-button>
            </div>
          </div>
          <el-empty v-if="providers.length === 0" size="small" description="暂无服务商" />
        </div>
      </div>

      <!-- 模型 -->
      <div class="config-section">
        <div class="section-title">
          <span>模型</span>
          <el-button type="success" size="small" :disabled="!currentProviderId" @click="handleAddModel">+ 添加</el-button>
        </div>
        <div class="card-list">
          <template v-if="currentProviderId">
            <div
              v-for="item in providerModels"
              :key="item.id"
              class="card"
              :class="{ active: currentModelId === item.id }"
              @click="handleSelectModel(item)"
            >
              <div class="card-body">
                <div class="card-badge" v-if="item.modelBadge">{{ item.modelBadge }}</div>
                <div class="card-name">{{ item.displayName }}</div>
                <div class="card-code">{{ item.modelIdentifier }}</div>
                <div class="card-meta">
                  <span>{{ item.contextLength }}K</span>
                  <span class="sep">|</span>
                  <span :class="item.multimodal === 1 ? 'text-primary' : 'text-muted'">
                    {{ item.multimodal === 1 ? '多模态' : '文本' }}
                  </span>
                </div>
              </div>
              <div class="card-footer">
                <el-tag size="small" :type="item.isActive === 1 ? 'success' : 'danger'">
                  {{ item.isActive === 1 ? '启用' : '停用' }}
                </el-tag>
                <el-button size="small" type="primary" link @click.stop="handleEditModel(item)">编辑</el-button>
                <el-button size="small" type="danger" link @click.stop="handleDeleteModel(item.id)">删除</el-button>
              </div>
            </div>
            <el-empty v-if="providerModels.length === 0" size="small" description="暂无模型" />
          </template>
          <el-empty v-else size="small" description="请先选择服务商" />
        </div>
      </div>

      <!-- 服务配置 -->
      <div class="config-section">
        <div class="section-title">
          <span>服务配置</span>
          <el-button type="warning" size="small" :disabled="!currentModelId" @click="handleAddService">+ 添加</el-button>
        </div>
        <div class="card-list">
          <template v-if="currentModelId">
            <div v-for="item in modelServices" :key="item.id" class="card">
              <div class="card-body">
                <div class="card-row">
                  <el-tag size="small" type="info">{{ item.apiType }}</el-tag>
                  <el-tag size="small" :type="item.status === 1 ? 'success' : 'danger'">
                    {{ item.status === 1 ? '启用' : '禁用' }}
                  </el-tag>
                </div>
                <div class="card-url">{{ item.url }}</div>
                <div class="card-meta">优先级: {{ item.priority }}</div>
              </div>
              <div class="card-footer">
                <el-button size="small" type="primary" link @click.stop="handleEditService(item)">编辑</el-button>
                <el-button size="small" type="danger" link @click.stop="handleDeleteService(item.id)">删除</el-button>
              </div>
            </div>
            <el-empty v-if="modelServices.length === 0" size="small" description="暂无配置" />
          </template>
          <el-empty v-else size="small" description="请先选择模型" />
        </div>
      </div>
    </div>

    <!-- 弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="420px">
      <el-form :model="dialogForm" label-width="70px">
        <template v-if="dialogType === 'provider'">
          <el-form-item label="代码">
            <el-input v-model="dialogForm.providerCode" :disabled="!!dialogForm.id" />
          </el-form-item>
          <el-form-item label="名称">
            <el-input v-model="dialogForm.providerName" />
          </el-form-item>
          <el-form-item label="官网">
            <el-input v-model="dialogForm.website" />
          </el-form-item>
          <el-form-item label="描述">
            <el-input v-model="dialogForm.description" type="textarea" />
          </el-form-item>
          <el-form-item label="状态">
            <el-radio-group v-model="dialogForm.status">
              <el-radio :label="1">启用</el-radio>
              <el-radio :label="0">禁用</el-radio>
            </el-radio-group>
          </el-form-item>
        </template>
        <template v-else-if="dialogType === 'model'">
          <el-form-item label="标识">
            <el-input v-model="dialogForm.modelIdentifier" :disabled="!!dialogForm.id" />
          </el-form-item>
          <el-form-item label="名称">
            <el-input v-model="dialogForm.displayName" />
          </el-form-item>
          <el-form-item label="标签">
            <el-input v-model="dialogForm.modelBadge" />
          </el-form-item>
          <el-form-item label="上下文">
            <el-input-number v-model="dialogForm.contextLength" :min="1" :max="2048" /> K
          </el-form-item>
          <el-form-item label="多模态">
            <el-radio-group v-model="dialogForm.multimodal">
              <el-radio :label="1">是</el-radio>
              <el-radio :label="0">否</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="描述">
            <el-input v-model="dialogForm.description" type="textarea" />
          </el-form-item>
          <el-form-item label="状态">
            <el-radio-group v-model="dialogForm.isActive">
              <el-radio :label="1">启用</el-radio>
              <el-radio :label="0">停用</el-radio>
            </el-radio-group>
          </el-form-item>
        </template>
        <template v-else>
          <el-form-item label="服务商">
            <el-input :value="currentProvider?.providerName" disabled />
          </el-form-item>
          <el-form-item label="模型">
            <el-input :value="currentModel?.displayName" disabled />
          </el-form-item>
          <el-form-item label="API类型">
            <el-select v-model="dialogForm.apiType">
              <el-option label="OpenAI" value="openai" />
              <el-option label="Anthropic" value="anthropic" />
              <el-option label="Gemini" value="gemini" />
              <el-option label="智谱" value="zhipu" />
              <el-option label="MiniMax" value="minimax" />
              <el-option label="月之暗面" value="moonshot" />
              <el-option label="阿里云" value="qwen" />
              <el-option label="DeepSeek" value="deepseek" />
              <el-option label="腾讯云" value="hunyuan" />
              <el-option label="字节跳动" value="doubao" />
            </el-select>
          </el-form-item>
          <el-form-item label="API密钥">
            <el-input v-model="dialogForm.apiKey" type="password" show-password />
          </el-form-item>
          <el-form-item label="地址">
            <el-input v-model="dialogForm.url" />
          </el-form-item>
          <el-form-item label="优先级">
            <el-input-number v-model="dialogForm.priority" :min="0" :max="100" />
          </el-form-item>
          <el-form-item label="状态">
            <el-radio-group v-model="dialogForm.status">
              <el-radio :label="1">启用</el-radio>
              <el-radio :label="0">禁用</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="描述">
            <el-input v-model="dialogForm.description" type="textarea" />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as aiApi from '@/api/ai'

const providers = ref([])
const allModels = ref([])
const allServiceConfigs = ref([])

const selectedProvider = ref(null)
const selectedModel = ref(null)

const dialogVisible = ref(false)
const dialogType = ref('provider')
const dialogForm = ref({})

const currentProviderId = computed(() => selectedProvider.value?.id)
const currentModelId = computed(() => selectedModel.value?.id)
const currentProvider = computed(() => selectedProvider.value)
const currentModel = computed(() => selectedModel.value)

const dialogTitle = computed(() => {
  const titles = { provider: '服务商', model: '模型', service: '服务配置' }
  return dialogForm.value.id ? `编辑${titles[dialogType.value]}` : `添加${titles[dialogType.value]}`
})

const providerModels = computed(() => {
  if (!currentProviderId.value) return []
  const services = allServiceConfigs.value.filter(s => s.providerId === currentProviderId.value)
  const modelIds = [...new Set(services.map(s => s.modelId))]
  return allModels.value.filter(m => modelIds.includes(m.id))
})

const modelServices = computed(() => {
  if (!currentModelId.value) return []
  return allServiceConfigs.value.filter(s => s.modelId === currentModelId.value)
})

const colors = ['#6366f1', '#8b5cf6', '#a855f7', '#d946ef', '#ec4899', '#f43f5e', '#ef4444', '#f97316', '#eab308', '#84cc16', '#22c55e', '#10b981', '#14b8a6', '#06b6d4', '#0ea5e9', '#3b82f6']
function getColor(code) {
  let hash = 0
  for (let i = 0; i < code.length; i++) {
    hash = code.charCodeAt(i) + ((hash << 5) - hash)
  }
  return colors[Math.abs(hash) % colors.length]
}

async function loadData() {
  try {
    providers.value = await aiApi.listProviders()
    allModels.value = await aiApi.listModels()
    allServiceConfigs.value = await aiApi.listServiceConfigs()
  } catch (e) {
    console.error(e)
  }
}

function handleSelectProvider(item) {
  selectedProvider.value = item
  selectedModel.value = null
}

function handleSelectModel(item) {
  selectedModel.value = item
}

function openDialog(type, item = null) {
  dialogType.value = type
  dialogForm.value = item ? { ...item } : getEmptyForm(type)
  dialogVisible.value = true
}

function getEmptyForm(type) {
  if (type === 'provider') return { id: null, providerCode: '', providerName: '', website: '', description: '', status: 1 }
  if (type === 'model') return { id: null, modelIdentifier: '', displayName: '', description: '', modelBadge: '', contextLength: 128, multimodal: 0, isActive: 1 }
  return { id: null, providerId: currentProviderId.value, modelId: currentModelId.value, apiType: '', apiKey: '', url: '', priority: 0, status: 1, description: '' }
}

function handleAddProvider() { openDialog('provider') }
function handleEditProvider(item) { openDialog('provider', item) }
function handleAddModel() { openDialog('model') }
function handleEditModel(item) { openDialog('model', item) }
function handleAddService() { openDialog('service') }
function handleEditService(item) { openDialog('service', item) }

async function handleDeleteProvider(id) {
  await ElMessageBox.confirm('确认删除？')
  await aiApi.deleteProvider(id)
  ElMessage.success('删除成功')
  if (selectedProvider.value?.id === id) { selectedProvider.value = null; selectedModel.value = null }
  loadData()
}

async function handleDeleteModel(id) {
  await ElMessageBox.confirm('确认删除？')
  await aiApi.deleteModel(id)
  ElMessage.success('删除成功')
  if (selectedModel.value?.id === id) selectedModel.value = null
  loadData()
}

async function handleDeleteService(id) {
  await ElMessageBox.confirm('确认删除？')
  await aiApi.deleteServiceConfig(id)
  ElMessage.success('删除成功')
  loadData()
}

async function handleSave() {
  const { id } = dialogForm.value
  if (dialogType.value === 'provider') {
    id ? await aiApi.updateProvider(id, dialogForm.value) : await aiApi.createProvider(dialogForm.value)
  } else if (dialogType.value === 'model') {
    id ? await aiApi.updateModel(id, dialogForm.value) : await aiApi.createModel(dialogForm.value)
  } else {
    id ? await aiApi.updateServiceConfig(id, dialogForm.value) : await aiApi.createServiceConfig(dialogForm.value)
  }
  dialogVisible.value = false
  ElMessage.success('保存成功')
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.ai-config {
  padding: 20px;
  height: 100vh;
  box-sizing: border-box;
}

.config-header {
  margin-bottom: 16px;
}

.config-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1f1f1f;
}

.config-body {
  display: flex;
  gap: 16px;
  height: calc(100vh - 76px);
}

.config-section {
  flex: 1;
  background: #fafafa;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

.section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.section-title span {
  font-size: 14px;
  font-weight: 600;
  color: #1f1f1f;
}

.card-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: calc(100vh - 160px);
}

.card {
  background: #fff;
  border-radius: 10px;
  padding: 14px;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;
  flex-shrink: 0;
}

.card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.card.active {
  border-color: #6366f1;
  background: #fefeff;
}

.card-body {
  margin-bottom: 10px;
}

.card-content {
  display: flex;
  align-items: center;
  gap: 10px;
}

.card-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.card-info {
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.card-info .card-name,
.card-info .card-code {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-tag {
  flex-shrink: 0;
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.card-row {
  display: flex;
  gap: 6px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.card-badge {
  display: inline-block;
  font-size: 11px;
  color: #6366f1;
  background: #eef2ff;
  padding: 2px 8px;
  border-radius: 4px;
  margin-bottom: 6px;
}

.card-row {
  display: flex;
  gap: 6px;
  margin-bottom: 8px;
}

.card-badge {
  display: inline-block;
  font-size: 11px;
  color: #6366f1;
  background: #eef2ff;
  padding: 2px 8px;
  border-radius: 4px;
  margin-bottom: 6px;
}

.card-name {
  font-size: 14px;
  font-weight: 600;
  color: #1f1f1f;
  margin-bottom: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-code {
  font-size: 12px;
  color: #8c8c8c;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-meta {
  font-size: 12px;
  color: #8c8c8c;
}

.card-meta .sep {
  margin: 0 4px;
}

.card-url {
  font-size: 11px;
  color: #666;
  background: #f5f5f5;
  padding: 6px 8px;
  border-radius: 4px;
  margin-bottom: 6px;
  word-break: break-all;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-top: 10px;
  border-top: 1px solid #f0f0f0;
}

.text-primary {
  color: #6366f1;
}

.text-muted {
  color: #8c8c8c;
}
</style>
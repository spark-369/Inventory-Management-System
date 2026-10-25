export interface CategoryDto {
  id?: number
  name: string
  description?: string
  imageUrl?: string
}

export interface BrandDto {
  id?: number
  name: string
  description?: string
  logoUrl?: string
}

export interface ProductDto {
  id?: number
  sku: string
  name: string
  description?: string
  categoryId?: number
  categoryName?: string
  brandId?: number
  brandName?: string
  supplierId?: number
  supplierName?: string
  barcode?: string
  qrCode?: string
  imageUrl?: string
  purchasePrice?: number
  sellingPrice?: number
  mrp?: number
  reorderLevel?: number
  currentStock?: number
  unit?: string
  active?: boolean
  trackBatch?: boolean
  trackExpiry?: boolean
}

export interface PurchaseItemDto {
  id?: number
  productId: number
  productName?: string
  quantity: number
  unitPrice?: number
  totalPrice?: number
  batchNumber?: string
}

export type PurchaseStatus = 'DRAFT' | 'PENDING' | 'RECEIVED' | 'COMPLETED' | 'CANCELLED'

export interface PurchaseDto {
  id?: number
  purchaseOrderNumber?: string
  supplierId?: number
  supplierName?: string
  warehouseId?: number
  warehouseName?: string
  purchaseDate?: string
  status?: PurchaseStatus
  totalAmount?: number
  notes?: string
  items?: PurchaseItemDto[]
}

export interface SaleItemDto {
  id?: number
  productId: number
  productName?: string
  quantity: number
  unitPrice?: number
  totalPrice?: number
  batchNumber?: string
}

export type SaleStatus = 'DRAFT' | 'PENDING' | 'COMPLETED' | 'CANCELLED' | 'RETURNED'

export interface SaleDto {
  id?: number
  invoiceNumber?: string
  customerId?: number
  customerName?: string
  warehouseId?: number
  warehouseName?: string
  saleDate?: string
  status?: SaleStatus
  totalAmount?: number
  discount?: number
  taxAmount?: number
  grandTotal?: number
  notes?: string
  items?: SaleItemDto[]
}

export type ReturnType = 'PURCHASE_RETURN' | 'SALE_RETURN'
export type ReturnStatus = 'PENDING' | 'APPROVED' | 'COMPLETED' | 'REJECTED'

export interface ReturnDto {
  id?: number
  returnNumber?: string
  type: ReturnType | string
  productId: number
  productName?: string
  warehouseId: number
  warehouseName?: string
  quantity?: number
  batchNumber?: string
  unitPrice?: number
  totalAmount?: number
  status?: ReturnStatus
  reason?: string
  returnDate?: string
}

export type MovementType =
  | 'IN'
  | 'OUT'
  | 'TRANSFER_IN'
  | 'TRANSFER_OUT'
  | 'ADJUSTMENT'
  | 'RETURN'
  | 'SALE'
  | 'PURCHASE'

export type ReferenceType = 'PURCHASE' | 'SALE' | 'TRANSFER' | 'ADJUSTMENT' | 'RETURN' | 'SYSTEM'

export interface StockMovementDto {
  id?: number
  productId: number
  productName?: string
  warehouseId: number
  warehouseName?: string
  movementType?: MovementType | string
  quantity?: number
  referenceType?: ReferenceType | string
  referenceId?: number
  notes?: string
  createdAt?: string
}

export interface StockBatchDto {
  id?: number
  productId: number
  productName?: string
  warehouseId: number
  warehouseName?: string
  batchNumber?: string
  expiryDate?: string
  quantity?: number
  costPrice?: number
  active?: boolean
}

export type AlertType = 'LOW_STOCK' | 'EXPIRY_WARNING' | 'EXPIRY_ALERT'

export interface AlertDto {
  id?: number
  type: AlertType | string
  message?: string
  productId?: number
  productName?: string
  batchNumber?: string
  expiryDate?: string
  resolved?: boolean
  createdAt?: string
  resolvedAt?: string
}

export interface WarehouseDto {
  id?: number
  name: string
  code?: string
  address?: string
  contactPerson?: string
  phone?: string
  active?: boolean
}

export interface CustomerDto {
  id?: number
  name: string
  email?: string
  phone?: string
  address?: string
  taxId?: string
  active?: boolean
}

export interface SupplierDto {
  id?: number
  name: string
  contactPerson?: string
  email?: string
  phone?: string
  address?: string
  taxId?: string
  active?: boolean
}

/* -------------------------------------------------------------------------- */
/* Messaging / background jobs (JobRunr broker)                               */
/* -------------------------------------------------------------------------- */

/** Live counters read from the JobRunr message broker. */
export interface JobStatsDto {
  timeStamp?: string
  total?: number
  awaiting?: number
  scheduled?: number
  enqueued?: number
  processing?: number
  failed?: number
  succeeded?: number
  allTimeSucceeded?: number
  deleted?: number
  recurringJobs?: number
  backgroundJobServers?: number
  /** Awaiting + scheduled + enqueued + processing. */
  pending?: number
  queryDurationInMillis?: number
  /** Deep link to the raw JobRunr dashboard, when it is enabled. */
  dashboardUrl?: string
}

export type JobMessageStatus = 'ENQUEUED' | 'RUNNING' | 'SUCCEEDED' | 'FAILED'

/** One row of the message audit trail. */
export interface JobMessageLogDto {
  id?: number
  correlationId?: string
  messageType?: string
  jobId?: string
  reason?: string
  requestedBy?: string
  status?: JobMessageStatus | string
  result?: string
  errorMessage?: string
  createdAt?: string
  processedAt?: string
  durationMillis?: number
}

/** A recurring schedule registered with the broker. */
export interface JobScheduleDto {
  id: string
  name: string
  schedule: string
  description?: string
  /** Message type accepted by the manual-dispatch endpoint. */
  trigger?: string
}

/** A message type the console may dispatch manually. */
export interface JobMessageTypeOption {
  value: string
  label: string
  description?: string
  /** False for messages only a domain workflow may produce (e.g. ORDER_CONFIRMATION). */
  dispatchable?: boolean
}

/** Response returned by the manual-dispatch endpoint. */
export interface JobDispatchResult {
  correlationId?: string
  messageType?: string
  status?: string
}

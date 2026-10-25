import { configureStore } from '@reduxjs/toolkit'
import categoriesReducer from './categoriesSlice'
import brandsReducer from './brandsSlice'
import productsReducer from './productsSlice'
import purchasesReducer from './purchasesSlice'
import salesReducer from './salesSlice'
import returnsReducer from './returnsSlice'
import stockMovementsReducer from './stockMovementsSlice'
import stockBatchesReducer from './stockBatchesSlice'
import alertsReducer from './alertsSlice'
import warehousesReducer from './warehousesSlice'
import customersReducer from './customersSlice'
import suppliersReducer from './suppliersSlice'
import jobsReducer from './jobsSlice'

export const store = configureStore({
  reducer: {
    categories: categoriesReducer,
    brands: brandsReducer,
    products: productsReducer,
    purchases: purchasesReducer,
    sales: salesReducer,
    returns: returnsReducer,
    stockMovements: stockMovementsReducer,
    stockBatches: stockBatchesReducer,
    alerts: alertsReducer,
    warehouses: warehousesReducer,
    customers: customersReducer,
    suppliers: suppliersReducer,
    jobs: jobsReducer,
  },
})

export type RootState = ReturnType<typeof store.getState>
export type AppDispatch = typeof store.dispatch

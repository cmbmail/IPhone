import { ApiGet } from './request'

export interface SearchResult {
  type: string
  id: number
  label: string
  subLabel: string
  route: string
}

export const searchApi = {
  globalSearch: (q: string) => ApiGet<SearchResult[]>('/search', { params: { q } }),
}

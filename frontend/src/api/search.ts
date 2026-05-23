import { request } from './request'

export const searchApi = {
  globalSearch: (q: string) => request.get('/search', { params: { q } }),
}

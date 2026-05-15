import { Tree } from 'antd'
import type { DataNode } from 'antd/es/tree'
import { useQuery } from '@tanstack/react-query'
import { orgApi } from '@/api/org'
import type { OrgStructure } from '@/types/org'

const OrgTree = () => {
  const { data: treeData, isLoading } = useQuery({
    queryKey: ['orgTree'],
    queryFn: async () => {
      const response = await orgApi.getTree()
      return response.data
    }
  })

  const convertToTreeData = (orgs: OrgStructure[]): DataNode[] => {
    return orgs.map(org => ({
      key: org.id,
      title: org.name,
      children: org.children ? convertToTreeData(org.children) : undefined
    }))
  }

  return (
    <Tree
      treeData={treeData ? convertToTreeData(treeData) : []}
      loading={isLoading}
      showLine={{ showLeafIcon: false }}
    />
  )
}

export default OrgTree

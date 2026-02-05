import React, { useCallback, useEffect, useState } from 'react';
import { Button, Card, Empty, Space, Spin, Typography, message } from 'antd';
import { useNavigate } from 'react-router-dom';
import { apiFetch } from '../api/client';
import type { DeviceGroupInfo } from '@shared/types';

const { Text } = Typography;

export function GroupsPage() {
  const [groups, setGroups] = useState<DeviceGroupInfo[]>([]);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const loadGroups = useCallback(async () => {
    setLoading(true);
    try {
      const data = await apiFetch<DeviceGroupInfo[]>('/api/groups');
      setGroups(data ?? []);
    } catch (error) {
      message.error((error as Error).message || '加载失败');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadGroups();
  }, [loadGroups]);

  return (
    <div className="h5-list">
      <Space style={{ width: '100%', justifyContent: 'space-between' }}>
        <Text>设备分组</Text>
        <Button size="small" onClick={loadGroups} loading={loading}>刷新</Button>
      </Space>

      {loading ? (
        <Spin />
      ) : groups.length === 0 ? (
        <Empty description="暂无可用分组" />
      ) : (
        groups.map((group) => (
          <Card key={group.id} size="small" className="h5-card">
            <div className="device-row">
              <div>
                <div>{group.name}</div>
                <div className="h5-group-meta">
                  设备数 {group.deviceCount}
                  {group.remark ? ` · ${group.remark}` : ''}
                </div>
              </div>
              <Button
                type="primary"
                size="small"
                onClick={() => navigate(`/groups/${group.id}`)}
              >
                进入
              </Button>
            </div>
          </Card>
        ))
      )}
    </div>
  );
}

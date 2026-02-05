import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Button, Card, Empty, Modal, Space, Spin, Tag, Typography, message } from 'antd';
import { useNavigate, useParams } from 'react-router-dom';
import { apiFetch, API_BASE } from '../api/client';
import type { DeviceDto, DeviceGroupDetail } from '@shared/types';
import { StreamPane } from '@shared/components/StreamPane';
import { getToken } from '../auth/storage';

const { Text } = Typography;

const formatDeviceTitle = (device: DeviceDto) => {
  if (device.alias) return device.alias;
  const label = `${device.manufacturer ?? ''} ${device.model ?? ''}`.trim();
  return label || device.deviceId;
};

export function GroupDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [group, setGroup] = useState<DeviceGroupDetail | null>(null);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<DeviceDto | null>(null);

  useEffect(() => {
    if (!selected) {
      if (screen.orientation?.unlock) {
        screen.orientation.unlock();
      }
      return;
    }
    if (screen.orientation?.lock) {
      screen.orientation.lock('portrait').catch(() => {});
    }
    return () => {
      if (screen.orientation?.unlock) {
        screen.orientation.unlock();
      }
    };
  }, [selected]);

  const loadDetail = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    try {
      const data = await apiFetch<DeviceGroupDetail>(`/api/groups/${id}`);
      setGroup(data ?? null);
    } catch (error) {
      message.error((error as Error).message || '加载失败');
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    loadDetail();
  }, [loadDetail]);

  const streamDevice = useMemo(() => {
    if (!selected) return null;
    return {
      deviceId: selected.deviceId,
      manufacturer: selected.manufacturer,
      model: selected.model
    };
  }, [selected]);

  return (
    <div className="h5-list">
      <Space style={{ width: '100%', justifyContent: 'space-between' }}>
        <Space>
          <Button size="small" onClick={() => navigate(-1)}>返回</Button>
          <Text>{group?.name ?? '设备分组'}</Text>
        </Space>
        <Button size="small" onClick={loadDetail} loading={loading}>刷新</Button>
      </Space>

      {loading ? (
        <Spin />
      ) : !group ? (
        <Empty description="分组不存在或无权限" />
      ) : group.devices.length === 0 ? (
        <Empty description="分组暂无设备" />
      ) : (
        group.devices.map((device) => (
          <Card key={device.deviceId} size="small" className="h5-card">
            <div className="device-card">
              <div className="device-row">
                <div>
                  <div>{formatDeviceTitle(device)}</div>
                  <div className="device-meta">
                    {device.deviceId}
                  </div>
                </div>
                <Space>
                  <Tag color={device.state === 'device' ? 'green' : 'default'}>
                    {device.state}
                  </Tag>
                  <Button
                    type="primary"
                    size="small"
                    disabled={device.state !== 'device'}
                    onClick={() => setSelected(device)}
                  >
                    连接
                  </Button>
                </Space>
              </div>
              {device.remark ? <div className="device-meta">{device.remark}</div> : null}
            </div>
          </Card>
        ))
      )}

      <Modal
        open={Boolean(selected)}
        onCancel={() => setSelected(null)}
        footer={null}
        destroyOnHidden
        width="100%"
        style={{ top: 0, padding: 0 }}
        className="h5-stream-modal"
        styles={{
          body: {
            padding: 0,
            height: '100vh',
            display: 'flex',
            flexDirection: 'column'
          },
          content: {
            padding: 0
          }
        }}
      >
        <div className="h5-stream">
          <StreamPane device={streamDevice} apiBase={API_BASE} tokenProvider={getToken} />
        </div>
      </Modal>
    </div>
  );
}

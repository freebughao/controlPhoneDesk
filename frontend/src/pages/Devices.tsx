import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Button, Drawer, Form, Input, Modal, Space, Table, Tag, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { apiFetch, API_BASE } from '../api/client';
import type { DeviceDto } from '../types';
import { StreamPane } from '../components/StreamPane';
import { useAuth } from '../auth/AuthContext';

export function DevicesPage() {
  const { user } = useAuth();
  const [devices, setDevices] = useState<DeviceDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [editing, setEditing] = useState<DeviceDto | null>(null);
  const [connecting, setConnecting] = useState<DeviceDto | null>(null);
  const [form] = Form.useForm<{ alias?: string; remark?: string }>();
  const canEdit = user?.superAdmin || user?.permissions?.includes('device:update');
  const canConnect = user?.superAdmin || user?.permissions?.includes('device:connect');

  const loadDevices = useCallback(async () => {
    setLoading(true);
    try {
      const data = await apiFetch<DeviceDto[]>('/api/devices');
      setDevices(data);
    } catch (error) {
      message.error((error as Error).message || '加载设备失败');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadDevices();
  }, [loadDevices]);

  const openEdit = useCallback((record: DeviceDto) => {
    setEditing(record);
    form.setFieldsValue({ alias: record.alias, remark: record.remark });
  }, [form]);

  const saveEdit = useCallback(async () => {
    const values = await form.validateFields();
    if (!editing) return;
    try {
      await apiFetch(`/api/devices/${encodeURIComponent(editing.deviceId)}`, {
        method: 'PUT',
        body: JSON.stringify(values)
      });
      message.success('设备信息已更新');
      setEditing(null);
      loadDevices();
    } catch (error) {
      message.error((error as Error).message || '更新失败');
    }
  }, [editing, form, loadDevices]);

  const connectDevice = useCallback(async (record: DeviceDto) => {
    try {
      await apiFetch(`/api/devices/${encodeURIComponent(record.deviceId)}/scrcpy/start`, {
        method: 'POST'
      });
      setConnecting(record);
    } catch (error) {
      message.error((error as Error).message || '连接失败');
    }
  }, []);

  const columns = useMemo<ColumnsType<DeviceDto>>(() => [
    { title: '设备ID', dataIndex: 'deviceId', key: 'deviceId', width: 200 },
    {
      title: '状态',
      dataIndex: 'state',
      key: 'state',
      width: 120,
      render: (state: string) => (
        <Tag color={state === 'device' ? 'green' : 'default'}>{state}</Tag>
      )
    },
    { title: '别名', dataIndex: 'alias', key: 'alias', width: 160 },
    { title: '备注', dataIndex: 'remark', key: 'remark', width: 200 },
    { title: '品牌', dataIndex: 'manufacturer', key: 'manufacturer', width: 120 },
    { title: '型号', dataIndex: 'model', key: 'model', width: 160 },
    {
      title: '分组',
      key: 'groups',
      render: (_, record) => record.groups?.map((g) => (
        <Tag key={g.id}>{g.name}</Tag>
      ))
    },
    {
      title: '操作',
      key: 'actions',
      width: 180,
      render: (_, record) => (
        <Space>
          {canConnect && (
            <Button size="small" type="link" onClick={() => connectDevice(record)}>
              连接
            </Button>
          )}
          {canEdit && (
            <Button size="small" type="link" onClick={() => openEdit(record)}>
              编辑
            </Button>
          )}
        </Space>
      )
    }
  ], [canConnect, canEdit, connectDevice, openEdit]);

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-title">设备管理</div>
        <Button onClick={loadDevices} loading={loading}>刷新</Button>
      </div>
      <Table
        rowKey="deviceId"
        columns={columns}
        dataSource={devices}
        loading={loading}
        pagination={{ pageSize: 10 }}
      />

      <Modal
        title="编辑设备"
        open={!!editing}
        onCancel={() => setEditing(null)}
        onOk={saveEdit}
        okText="保存"
      >
        <Form form={form} layout="vertical">
          <Form.Item label="别名" name="alias">
            <Input placeholder="请输入别名" />
          </Form.Item>
          <Form.Item label="备注" name="remark">
            <Input.TextArea rows={3} placeholder="请输入备注" />
          </Form.Item>
        </Form>
      </Modal>

      <Drawer
        title="设备投屏"
        open={!!connecting}
        onClose={() => setConnecting(null)}
        width="70%"
        destroyOnClose
      >
        {connecting && (
          <StreamPane
            apiBase={API_BASE}
            device={{
              deviceId: connecting.deviceId,
              manufacturer: connecting.manufacturer,
              model: connecting.model
            }}
          />
        )}
      </Drawer>
    </div>
  );
}

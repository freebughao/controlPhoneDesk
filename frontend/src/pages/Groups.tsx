import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Button, Drawer, Form, Input, Modal, Select, Space, Table, Tag, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { apiFetch, API_BASE } from '../api/client';
import type { DeviceDto, DeviceGroupDetail, DeviceGroupInfo, DeviceInfo } from '../types';
import { StreamPane } from '../components/StreamPane';
import { useAuth } from '../auth/AuthContext';

export function GroupsPage() {
  const { user } = useAuth();
  const [groups, setGroups] = useState<DeviceGroupInfo[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedGroup, setSelectedGroup] = useState<DeviceGroupDetail | null>(null);
  const [editingGroup, setEditingGroup] = useState<DeviceGroupInfo | null>(null);
  const [showGroupModal, setShowGroupModal] = useState(false);
  const [deviceModalOpen, setDeviceModalOpen] = useState(false);
  const [adbDevices, setAdbDevices] = useState<DeviceInfo[]>([]);
  const [multiConnectOpen, setMultiConnectOpen] = useState(false);
  const [connectingDevice, setConnectingDevice] = useState<DeviceDto | null>(null);
  const [groupForm] = Form.useForm<{ name: string; remark?: string }>();
  const [deviceForm] = Form.useForm<{ deviceId: string; alias?: string; remark?: string }>();
  const canCreateGroup = user?.superAdmin || user?.permissions?.includes('group:create');
  const canEditGroup = user?.superAdmin || user?.permissions?.includes('group:update');
  const canDeleteGroup = user?.superAdmin || user?.permissions?.includes('group:delete');
  const canAddDevice = user?.superAdmin || user?.permissions?.includes('group:device:add');
  const canRemoveDevice = user?.superAdmin || user?.permissions?.includes('group:device:remove');
  const canConnect = user?.superAdmin || user?.permissions?.includes('device:connect');

  const loadGroups = useCallback(async () => {
    setLoading(true);
    try {
      const data = await apiFetch<DeviceGroupInfo[]>('/api/groups');
      setGroups(data);
    } catch (error) {
      message.error((error as Error).message || '加载分组失败');
    } finally {
      setLoading(false);
    }
  }, []);

  const loadGroupDetail = useCallback(async (groupId: number) => {
    try {
      const detail = await apiFetch<DeviceGroupDetail>(`/api/groups/${groupId}`);
      setSelectedGroup(detail);
    } catch (error) {
      message.error((error as Error).message || '加载分组详情失败');
    }
  }, []);

  useEffect(() => {
    loadGroups();
  }, [loadGroups]);

  const openCreateGroup = useCallback(() => {
    setEditingGroup(null);
    groupForm.resetFields();
    setShowGroupModal(true);
  }, [groupForm]);

  const openEditGroup = useCallback((group: DeviceGroupInfo) => {
    setEditingGroup(group);
    groupForm.setFieldsValue({ name: group.name, remark: group.remark });
    setShowGroupModal(true);
  }, [groupForm]);

  const saveGroup = useCallback(async () => {
    const values = await groupForm.validateFields();
    try {
      if (editingGroup) {
        await apiFetch(`/api/groups/${editingGroup.id}`, {
          method: 'PUT',
          body: JSON.stringify(values)
        });
        message.success('分组已更新');
      } else {
        await apiFetch('/api/groups', {
          method: 'POST',
          body: JSON.stringify(values)
        });
        message.success('分组已创建');
      }
      setShowGroupModal(false);
      loadGroups();
    } catch (error) {
      message.error((error as Error).message || '保存分组失败');
    }
  }, [editingGroup, groupForm, loadGroups]);

  const deleteGroup = useCallback(async (groupId: number) => {
    try {
      await apiFetch(`/api/groups/${groupId}`, { method: 'DELETE' });
      if (selectedGroup?.id === groupId) {
        setSelectedGroup(null);
      }
      message.success('分组已删除');
      loadGroups();
    } catch (error) {
      message.error((error as Error).message || '删除失败');
    }
  }, [loadGroups, selectedGroup?.id]);

  const openAddDevice = useCallback(async () => {
    if (!selectedGroup) {
      message.warning('请先选择分组');
      return;
    }
    try {
      const adbList = await apiFetch<DeviceInfo[]>('/api/devices/adb');
      setAdbDevices(adbList);
      deviceForm.resetFields();
      setDeviceModalOpen(true);
    } catch (error) {
      message.error((error as Error).message || '获取设备列表失败');
    }
  }, [deviceForm, selectedGroup]);

  const connectGroupDevices = useCallback(async () => {
    if (!selectedGroup || !selectedGroup.devices?.length) return;
    try {
      await Promise.all(
        selectedGroup.devices.map((device) =>
          apiFetch(`/api/devices/${encodeURIComponent(device.deviceId)}/scrcpy/start`, { method: 'POST' })
        )
      );
      setMultiConnectOpen(true);
    } catch (error) {
      message.error((error as Error).message || '连接失败');
    }
  }, [selectedGroup]);

  const connectSingleDevice = useCallback(async (device: DeviceDto) => {
    try {
      await apiFetch(`/api/devices/${encodeURIComponent(device.deviceId)}/scrcpy/start`, { method: 'POST' });
      setConnectingDevice(device);
    } catch (error) {
      message.error((error as Error).message || '连接失败');
    }
  }, []);

  const addDeviceToGroup = useCallback(async () => {
    if (!selectedGroup) return;
    const values = await deviceForm.validateFields();
    try {
      await apiFetch(`/api/groups/${selectedGroup.id}/devices`, {
        method: 'POST',
        body: JSON.stringify(values)
      });
      message.success('设备已添加');
      setDeviceModalOpen(false);
      loadGroupDetail(selectedGroup.id);
      loadGroups();
    } catch (error) {
      message.error((error as Error).message || '添加失败');
    }
  }, [deviceForm, loadGroupDetail, loadGroups, selectedGroup]);

  const removeDevice = useCallback(async (deviceId: string) => {
    if (!selectedGroup) return;
    try {
      await apiFetch(`/api/groups/${selectedGroup.id}/devices/${encodeURIComponent(deviceId)}`, {
        method: 'DELETE'
      });
      message.success('已移除设备');
      loadGroupDetail(selectedGroup.id);
      loadGroups();
    } catch (error) {
      message.error((error as Error).message || '移除失败');
    }
  }, [loadGroupDetail, loadGroups, selectedGroup]);

  const groupColumns = useMemo<ColumnsType<DeviceGroupInfo>>(() => [
    { title: '分组名称', dataIndex: 'name', key: 'name' },
    { title: '备注', dataIndex: 'remark', key: 'remark' },
    { title: '设备数', dataIndex: 'deviceCount', key: 'deviceCount', width: 100 },
    {
      title: '操作',
      key: 'actions',
      width: 180,
      render: (_, record) => (
        <Space>
          <Button size="small" type="link" onClick={() => loadGroupDetail(record.id)}>查看</Button>
          {canEditGroup && (
            <Button size="small" type="link" onClick={() => openEditGroup(record)}>编辑</Button>
          )}
          {canDeleteGroup && (
            <Button size="small" type="link" danger onClick={() => deleteGroup(record.id)}>删除</Button>
          )}
        </Space>
      )
    }
  ], [canDeleteGroup, canEditGroup, deleteGroup, loadGroupDetail, openEditGroup]);

  const deviceColumns = useMemo<ColumnsType<DeviceDto>>(() => [
    { title: '设备ID', dataIndex: 'deviceId', key: 'deviceId', width: 220 },
    {
      title: '状态',
      dataIndex: 'state',
      key: 'state',
      width: 120,
      render: (state: string) => (
        <Tag color={state === 'device' ? 'green' : 'default'}>{state}</Tag>
      )
    },
    { title: '别名', dataIndex: 'alias', key: 'alias', width: 140 },
    { title: '备注', dataIndex: 'remark', key: 'remark' },
    {
      title: '操作',
      key: 'action',
      width: 180,
      render: (_, record) => (
        <Space>
          {canConnect && (
            <Button size="small" type="link" onClick={() => connectSingleDevice(record)}>
              单独连接
            </Button>
          )}
          {canRemoveDevice && (
            <Button size="small" type="link" danger onClick={() => removeDevice(record.deviceId)}>
              移除
            </Button>
          )}
        </Space>
      )
    }
  ], [canConnect, canRemoveDevice, connectSingleDevice, removeDevice]);

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-title">设备分组管理</div>
        <Space>
          <Button onClick={loadGroups} loading={loading}>刷新</Button>
          {canCreateGroup && (
            <Button type="primary" onClick={openCreateGroup}>新增分组</Button>
          )}
        </Space>
      </div>

      <Table
        rowKey="id"
        columns={groupColumns}
        dataSource={groups}
        loading={loading}
        pagination={{ pageSize: 8 }}
      />

      <div className="group-detail">
        <div className="group-detail-header">
          <div className="page-title">分组设备</div>
          <Space>
            {canAddDevice && (
              <Button onClick={openAddDevice} disabled={!selectedGroup}>添加设备</Button>
            )}
            {canConnect && (
              <Button type="primary" disabled={!selectedGroup?.devices?.length} onClick={connectGroupDevices}>
                连接本组设备
              </Button>
            )}
          </Space>
        </div>
        <Table
          rowKey="deviceId"
          columns={deviceColumns}
          dataSource={selectedGroup?.devices ?? []}
          pagination={{ pageSize: 6 }}
        />
      </div>

      <Modal
        title={editingGroup ? '编辑分组' : '新增分组'}
        open={showGroupModal}
        onCancel={() => setShowGroupModal(false)}
        onOk={saveGroup}
        okText="保存"
      >
        <Form form={groupForm} layout="vertical">
          <Form.Item name="name" label="分组名称" rules={[{ required: true, message: '请输入分组名称' }]}>
            <Input placeholder="例如：测试组" />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={3} placeholder="请输入备注" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="添加设备到分组"
        open={deviceModalOpen}
        onCancel={() => setDeviceModalOpen(false)}
        onOk={addDeviceToGroup}
        okText="添加"
      >
        <Form form={deviceForm} layout="vertical">
          <Form.Item name="deviceId" label="选择设备" rules={[{ required: true, message: '请选择设备' }]}>
            <Select
              showSearch
              optionFilterProp="label"
              options={adbDevices.map((item) => ({
                label: `${item.udid} (${item.model || item.manufacturer || 'Android'})`,
                value: item.udid
              }))}
            />
          </Form.Item>
          <Form.Item name="alias" label="别名">
            <Input />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>

      <Drawer
        title={`分组投屏：${selectedGroup?.name ?? ''}`}
        open={multiConnectOpen}
        onClose={() => setMultiConnectOpen(false)}
        width="90%"
        destroyOnClose
      >
        <div className="multi-stream-grid">
          {(selectedGroup?.devices ?? []).map((device) => (
            <div key={device.deviceId} className="multi-stream-item">
              <StreamPane
                apiBase={API_BASE}
                device={{
                  deviceId: device.deviceId,
                  manufacturer: device.manufacturer,
                  model: device.model
                }}
              />
            </div>
          ))}
        </div>
      </Drawer>

      <Drawer
        title="设备投屏"
        open={!!connectingDevice}
        onClose={() => setConnectingDevice(null)}
        width="70%"
        destroyOnClose
      >
        {connectingDevice && (
          <StreamPane
            apiBase={API_BASE}
            device={{
              deviceId: connectingDevice.deviceId,
              manufacturer: connectingDevice.manufacturer,
              model: connectingDevice.model
            }}
          />
        )}
      </Drawer>
    </div>
  );
}

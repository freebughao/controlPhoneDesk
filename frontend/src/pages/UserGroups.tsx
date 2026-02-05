import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Button, Form, Input, Modal, Select, Space, Table, Tag, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { apiFetch } from '../api/client';
import type { DeviceGroupInfo, UserGroupDetail, UserGroupInfo, UserInfo, UserRef } from '../types';

type GroupFormValues = {
  name: string;
  remark?: string;
};

export function UserGroupsPage() {
  const [groups, setGroups] = useState<UserGroupInfo[]>([]);
  const [selectedGroup, setSelectedGroup] = useState<UserGroupDetail | null>(null);
  const [loading, setLoading] = useState(false);
  const [editingGroup, setEditingGroup] = useState<UserGroupInfo | null>(null);
  const [groupModalOpen, setGroupModalOpen] = useState(false);
  const [assignUsersOpen, setAssignUsersOpen] = useState(false);
  const [assignDeviceGroupsOpen, setAssignDeviceGroupsOpen] = useState(false);
  const [users, setUsers] = useState<UserRef[]>([]);
  const [deviceGroups, setDeviceGroups] = useState<DeviceGroupInfo[]>([]);
  const [groupForm] = Form.useForm<GroupFormValues>();
  const [assignUsersForm] = Form.useForm<{ userIds: number[] }>();
  const [assignDeviceGroupsForm] = Form.useForm<{ deviceGroupIds: number[] }>();
  const userOptions = useMemo(
    () =>
      users.map((user) => ({
        label: `${user.username}${user.superAdmin ? '（超级管理员）' : ''}`,
        value: user.id
      })),
    [users]
  );
  const deviceGroupOptions = useMemo(
    () =>
      deviceGroups.map((group) => ({
        label: group.name,
        value: group.id
      })),
    [deviceGroups]
  );

  const loadGroups = useCallback(async () => {
    setLoading(true);
    try {
      const data = await apiFetch<UserGroupInfo[]>('/api/user-groups');
      setGroups(data);
    } catch (error) {
      message.error((error as Error).message || '加载用户分组失败');
    } finally {
      setLoading(false);
    }
  }, []);

  const loadGroupDetail = useCallback(async (id: number) => {
    try {
      const detail = await apiFetch<UserGroupDetail>(`/api/user-groups/${id}`);
      setSelectedGroup(detail);
    } catch (error) {
      message.error((error as Error).message || '加载分组详情失败');
    }
  }, []);

  const loadUsers = useCallback(async () => {
    try {
      const data = await apiFetch<UserInfo[]>('/api/users');
      setUsers(
        data.map((item) => ({
          id: item.id,
          username: item.username,
          status: item.status,
          superAdmin: item.superAdmin
        }))
      );
    } catch (error) {
      message.error((error as Error).message || '加载用户失败');
    }
  }, []);

  const loadDeviceGroups = useCallback(async () => {
    try {
      const data = await apiFetch<DeviceGroupInfo[]>('/api/groups');
      setDeviceGroups(data);
    } catch (error) {
      message.error((error as Error).message || '加载设备分组失败');
    }
  }, []);

  useEffect(() => {
    loadGroups();
    void Promise.all([loadUsers(), loadDeviceGroups()]);
  }, [loadDeviceGroups, loadGroups, loadUsers]);

  const openCreateGroup = useCallback(() => {
    setEditingGroup(null);
    groupForm.resetFields();
    setGroupModalOpen(true);
  }, [groupForm]);

  const openEditGroup = useCallback((group: UserGroupInfo) => {
    setEditingGroup(group);
    groupForm.setFieldsValue({ name: group.name, remark: group.remark });
    setGroupModalOpen(true);
  }, [groupForm]);

  const saveGroup = useCallback(async () => {
    const values = await groupForm.validateFields();
    try {
      if (editingGroup) {
        await apiFetch(`/api/user-groups/${editingGroup.id}`, {
          method: 'PUT',
          body: JSON.stringify(values)
        });
        message.success('用户分组已更新');
      } else {
        await apiFetch('/api/user-groups', {
          method: 'POST',
          body: JSON.stringify(values)
        });
        message.success('用户分组已创建');
      }
      setGroupModalOpen(false);
      loadGroups();
    } catch (error) {
      message.error((error as Error).message || '保存失败');
    }
  }, [editingGroup, groupForm, loadGroups]);

  const deleteGroup = useCallback(async (groupId: number) => {
    try {
      await apiFetch(`/api/user-groups/${groupId}`, { method: 'DELETE' });
      if (selectedGroup?.id === groupId) {
        setSelectedGroup(null);
      }
      message.success('用户分组已删除');
      loadGroups();
    } catch (error) {
      message.error((error as Error).message || '删除失败');
    }
  }, [loadGroups, selectedGroup?.id]);

  const openAssignUsers = useCallback(() => {
    if (!selectedGroup) {
      message.warning('请先选择用户分组');
      return;
    }
    assignUsersForm.setFieldsValue({
      userIds: selectedGroup.users.map((user) => user.id)
    });
    setAssignUsersOpen(true);
  }, [assignUsersForm, selectedGroup]);

  const saveAssignUsers = useCallback(async () => {
    if (!selectedGroup) return;
    const values = await assignUsersForm.validateFields();
    try {
      const detail = await apiFetch<UserGroupDetail>(`/api/user-groups/${selectedGroup.id}/users`, {
        method: 'PUT',
        body: JSON.stringify(values)
      });
      setSelectedGroup(detail);
      message.success('成员已更新');
      setAssignUsersOpen(false);
      loadGroups();
    } catch (error) {
      message.error((error as Error).message || '更新失败');
    }
  }, [assignUsersForm, loadGroups, selectedGroup]);

  const openAssignDeviceGroups = useCallback(() => {
    if (!selectedGroup) {
      message.warning('请先选择用户分组');
      return;
    }
    assignDeviceGroupsForm.setFieldsValue({
      deviceGroupIds: selectedGroup.deviceGroups.map((group) => group.id)
    });
    setAssignDeviceGroupsOpen(true);
  }, [assignDeviceGroupsForm, selectedGroup]);

  const saveAssignDeviceGroups = useCallback(async () => {
    if (!selectedGroup) return;
    const values = await assignDeviceGroupsForm.validateFields();
    try {
      const detail = await apiFetch<UserGroupDetail>(`/api/user-groups/${selectedGroup.id}/device-groups`, {
        method: 'PUT',
        body: JSON.stringify(values)
      });
      setSelectedGroup(detail);
      message.success('设备范围已更新');
      setAssignDeviceGroupsOpen(false);
      loadGroups();
    } catch (error) {
      message.error((error as Error).message || '更新失败');
    }
  }, [assignDeviceGroupsForm, loadGroups, selectedGroup]);

  const groupColumns = useMemo<ColumnsType<UserGroupInfo>>(() => [
    { title: '分组名称', dataIndex: 'name', key: 'name' },
    { title: '备注', dataIndex: 'remark', key: 'remark' },
    { title: '成员数', dataIndex: 'userCount', key: 'userCount', width: 100 },
    { title: '设备分组数', dataIndex: 'deviceGroupCount', key: 'deviceGroupCount', width: 120 },
    {
      title: '操作',
      key: 'actions',
      width: 200,
      render: (_, record) => (
        <Space>
          <Button size="small" type="link" onClick={() => loadGroupDetail(record.id)}>查看</Button>
          <Button size="small" type="link" onClick={() => openEditGroup(record)}>编辑</Button>
          <Button size="small" type="link" danger onClick={() => deleteGroup(record.id)}>删除</Button>
        </Space>
      )
    }
  ], [deleteGroup, loadGroupDetail, openEditGroup]);

  const userColumns = useMemo<ColumnsType<UserRef>>(() => [
    { title: '账号', dataIndex: 'username', key: 'username' },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 120,
      render: (status: UserRef['status']) => (
        <Tag color={status === 'ACTIVE' ? 'green' : 'default'}>{status === 'ACTIVE' ? '正常' : '停用'}</Tag>
      )
    },
    {
      title: '类型',
      key: 'type',
      width: 120,
      render: (_, record) => (
        <Tag color={record.superAdmin ? 'gold' : 'default'}>{record.superAdmin ? '超级管理员' : '普通账号'}</Tag>
      )
    }
  ], []);

  const deviceGroupColumns = useMemo<ColumnsType<DeviceGroupInfo>>(() => [
    { title: '分组名称', dataIndex: 'name', key: 'name' },
    { title: '备注', dataIndex: 'remark', key: 'remark' },
    { title: '设备数', dataIndex: 'deviceCount', key: 'deviceCount', width: 100 }
  ], []);

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-title">用户分组管理</div>
        <Space>
          <Button onClick={loadGroups} loading={loading}>刷新</Button>
          <Button type="primary" onClick={openCreateGroup}>新增分组</Button>
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
          <div className="page-title">分组成员与设备范围</div>
          <Space>
            <Button onClick={openAssignUsers} disabled={!selectedGroup}>设置成员</Button>
            <Button type="primary" onClick={openAssignDeviceGroups} disabled={!selectedGroup}>设置设备范围</Button>
          </Space>
        </div>
        <div style={{ display: 'grid', gap: 16, gridTemplateColumns: '1fr 1fr' }}>
          <Table
            rowKey="id"
            columns={userColumns}
            dataSource={selectedGroup?.users ?? []}
            pagination={{ pageSize: 6 }}
            title={() => `成员列表${selectedGroup ? `（${selectedGroup.name}）` : ''}`}
          />
          <Table
            rowKey="id"
            columns={deviceGroupColumns}
            dataSource={selectedGroup?.deviceGroups ?? []}
            pagination={{ pageSize: 6 }}
            title={() => '设备分组范围'}
          />
        </div>
      </div>

      <Modal
        title={editingGroup ? '编辑用户分组' : '新增用户分组'}
        open={groupModalOpen}
        onCancel={() => setGroupModalOpen(false)}
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
        title="设置分组成员"
        open={assignUsersOpen}
        onCancel={() => setAssignUsersOpen(false)}
        onOk={saveAssignUsers}
        okText="保存"
      >
        <Form form={assignUsersForm} layout="vertical">
          <Form.Item name="userIds" label="选择成员">
            <Select
              mode="multiple"
              optionFilterProp="label"
              options={userOptions}
            />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="设置设备分组范围"
        open={assignDeviceGroupsOpen}
        onCancel={() => setAssignDeviceGroupsOpen(false)}
        onOk={saveAssignDeviceGroups}
        okText="保存"
      >
        <Form form={assignDeviceGroupsForm} layout="vertical">
          <Form.Item name="deviceGroupIds" label="设备分组范围">
            <Select
              mode="multiple"
              optionFilterProp="label"
              options={deviceGroupOptions}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

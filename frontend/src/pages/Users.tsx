import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Button, Form, Input, Modal, Select, Space, Switch, Table, Tag, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { apiFetch } from '../api/client';
import type { RoleInfo, UserInfo } from '../types';

export function UsersPage() {
  const [users, setUsers] = useState<UserInfo[]>([]);
  const [roles, setRoles] = useState<RoleInfo[]>([]);
  const [loading, setLoading] = useState(false);
  const [createOpen, setCreateOpen] = useState(false);
  const [roleOpen, setRoleOpen] = useState(false);
  const [passwordOpen, setPasswordOpen] = useState(false);
  const [currentUser, setCurrentUser] = useState<UserInfo | null>(null);
  const [createForm] = Form.useForm();
  const [roleForm] = Form.useForm();
  const [passwordForm] = Form.useForm();

  const loadUsers = useCallback(async () => {
    setLoading(true);
    try {
      const data = await apiFetch<UserInfo[]>('/api/users');
      setUsers(data);
    } catch (error) {
      message.error((error as Error).message || '加载用户失败');
    } finally {
      setLoading(false);
    }
  }, []);

  const loadRoles = useCallback(async () => {
    try {
      const data = await apiFetch<{ id: number; code: string; name: string }[]>('/api/roles');
      setRoles(data.map((item) => ({ id: item.id, code: item.code, name: item.name })));
    } catch (error) {
      message.error((error as Error).message || '加载角色失败');
    }
  }, []);

  useEffect(() => {
    loadUsers();
    loadRoles();
  }, [loadRoles, loadUsers]);

  const openCreate = useCallback(() => {
    createForm.resetFields();
    setCreateOpen(true);
  }, [createForm]);

  const createUser = useCallback(async () => {
    const values = await createForm.validateFields();
    try {
      await apiFetch('/api/users', {
        method: 'POST',
        body: JSON.stringify(values)
      });
      message.success('用户已创建');
      setCreateOpen(false);
      loadUsers();
    } catch (error) {
      message.error((error as Error).message || '创建失败');
    }
  }, [createForm, loadUsers]);

  const toggleStatus = useCallback(async (user: UserInfo, checked: boolean) => {
    try {
      await apiFetch(`/api/users/${user.id}/status`, {
        method: 'PUT',
        body: JSON.stringify({ status: checked ? 'ACTIVE' : 'DISABLED' })
      });
      message.success('状态已更新');
      loadUsers();
    } catch (error) {
      message.error((error as Error).message || '更新失败');
    }
  }, [loadUsers]);

  const openAssignRoles = useCallback((user: UserInfo) => {
    setCurrentUser(user);
    roleForm.setFieldsValue({ roleIds: user.roles.map((role) => role.id) });
    setRoleOpen(true);
  }, [roleForm]);

  const assignRoles = useCallback(async () => {
    if (!currentUser) return;
    const values = await roleForm.validateFields();
    try {
      await apiFetch(`/api/users/${currentUser.id}/roles`, {
        method: 'PUT',
        body: JSON.stringify(values)
      });
      message.success('角色已更新');
      setRoleOpen(false);
      loadUsers();
    } catch (error) {
      message.error((error as Error).message || '更新失败');
    }
  }, [currentUser, loadUsers, roleForm]);

  const openChangePassword = useCallback((user: UserInfo) => {
    setCurrentUser(user);
    passwordForm.resetFields();
    setPasswordOpen(true);
  }, [passwordForm]);

  const changePassword = useCallback(async () => {
    if (!currentUser) return;
    const values = await passwordForm.validateFields();
    try {
      await apiFetch(`/api/users/${currentUser.id}/password`, {
        method: 'PUT',
        body: JSON.stringify(values)
      });
      message.success('密码已修改');
      setPasswordOpen(false);
    } catch (error) {
      message.error((error as Error).message || '修改失败');
    }
  }, [currentUser, passwordForm]);

  const deleteUser = useCallback(async (user: UserInfo) => {
    try {
      await apiFetch(`/api/users/${user.id}`, { method: 'DELETE' });
      message.success('用户已删除');
      loadUsers();
    } catch (error) {
      message.error((error as Error).message || '删除失败');
    }
  }, [loadUsers]);

  const columns = useMemo<ColumnsType<UserInfo>>(() => [
    { title: '账号', dataIndex: 'username', key: 'username' },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 120,
      render: (_, record) => (
        <Switch
          checkedChildren="正常"
          unCheckedChildren="停用"
          checked={record.status === 'ACTIVE'}
          disabled={record.superAdmin}
          onChange={(checked) => toggleStatus(record, checked)}
        />
      )
    },
    {
      title: '角色',
      key: 'roles',
      render: (_, record) => record.roles.map((role) => (
        <Tag key={role.id}>{role.name}</Tag>
      ))
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
      render: (value: string) => new Date(value).toLocaleString()
    },
    {
      title: '操作',
      key: 'actions',
      width: 240,
      render: (_, record) => (
        <Space>
          <Button size="small" type="link" onClick={() => openAssignRoles(record)} disabled={record.superAdmin}>
            分配角色
          </Button>
          <Button size="small" type="link" onClick={() => openChangePassword(record)}>
            修改密码
          </Button>
          <Button size="small" type="link" danger onClick={() => deleteUser(record)} disabled={record.superAdmin}>
            删除
          </Button>
        </Space>
      )
    }
  ], [deleteUser, openAssignRoles, openChangePassword, toggleStatus]);

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-title">用户管理</div>
        <Space>
          <Button onClick={loadUsers} loading={loading}>刷新</Button>
          <Button type="primary" onClick={openCreate}>新增用户</Button>
        </Space>
      </div>
      <Table rowKey="id" columns={columns} dataSource={users} loading={loading} />

      <Modal
        title="新增用户"
        open={createOpen}
        onCancel={() => setCreateOpen(false)}
        onOk={createUser}
        okText="创建"
      >
        <Form form={createForm} layout="vertical">
          <Form.Item name="username" label="账号" rules={[{ required: true, message: '请输入账号' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="password" label="密码" rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password />
          </Form.Item>
          <Form.Item name="roleIds" label="角色">
            <Select
              mode="multiple"
              options={roles.map((role) => ({ label: role.name, value: role.id }))}
            />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="分配角色"
        open={roleOpen}
        onCancel={() => setRoleOpen(false)}
        onOk={assignRoles}
        okText="保存"
      >
        <Form form={roleForm} layout="vertical">
          <Form.Item name="roleIds" label="角色">
            <Select
              mode="multiple"
              options={roles.map((role) => ({ label: role.name, value: role.id }))}
            />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="修改密码"
        open={passwordOpen}
        onCancel={() => setPasswordOpen(false)}
        onOk={changePassword}
        okText="保存"
      >
        <Form form={passwordForm} layout="vertical">
          <Form.Item name="password" label="新密码" rules={[{ required: true, message: '请输入新密码' }]}>
            <Input.Password />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

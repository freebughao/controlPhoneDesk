import React, { useMemo } from 'react';
import { Layout, Menu, Dropdown, Space, Typography } from 'antd';
import { UserOutlined, AppstoreOutlined, LaptopOutlined, TeamOutlined, LogoutOutlined, UsergroupAddOutlined } from '@ant-design/icons';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

const { Header, Sider, Content } = Layout;
const { Text } = Typography;

export function AdminLayout({ children }: { children: React.ReactNode }) {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout } = useAuth();

  const selectedKey = useMemo(() => {
    if (location.pathname.startsWith('/groups')) return '/groups';
    if (location.pathname.startsWith('/user-groups')) return '/user-groups';
    if (location.pathname.startsWith('/users')) return '/users';
    return '/devices';
  }, [location.pathname]);

  type NavItem = { key: string; icon: React.ReactNode; label: React.ReactNode; perm?: string };

  const menuItems = useMemo<NavItem[]>(() => ([
    { key: '/devices', icon: <LaptopOutlined />, label: <Link to="/devices">设备管理</Link>, perm: 'device:list' },
    { key: '/groups', icon: <AppstoreOutlined />, label: <Link to="/groups">设备分组</Link>, perm: 'group:list' },
    { key: '/user-groups', icon: <UsergroupAddOutlined />, label: <Link to="/user-groups">用户分组</Link>, perm: 'usergroup:list' },
    { key: '/users', icon: <TeamOutlined />, label: <Link to="/users">用户管理</Link>, perm: 'user:list' }
  ]), []);

  const visibleMenuItems = useMemo<NavItem[]>(() => {
    if (user?.superAdmin) {
      return menuItems;
    }
    const perms = new Set(user?.permissions ?? []);
    return menuItems.filter((item) => !item.perm || perms.has(item.perm));
  }, [menuItems, user?.permissions, user?.superAdmin]);

  const userMenu = useMemo(() => ({
    items: [
      { key: 'account', label: <Text>{user?.username ?? '未知账号'}</Text>, icon: <UserOutlined /> },
      { type: 'divider' as const },
      { key: 'logout', label: '退出登录', icon: <LogoutOutlined /> }
    ],
    onClick: ({ key }: { key: string }) => {
      if (key === 'logout') {
        logout();
        navigate('/login');
      }
    }
  }), [logout, navigate, user?.username]);

  return (
    <Layout className="admin-layout">
      <Sider width={220} theme="light">
        <div className="logo">新机管理后台</div>
        <Menu
          mode="inline"
          selectedKeys={[selectedKey]}
          items={visibleMenuItems.map(({ perm, ...item }) => item)}
        />
      </Sider>
      <Layout>
        <Header className="admin-header">
          <Dropdown menu={userMenu} placement="bottomRight">
            <Space className="admin-user">
              <UserOutlined />
              <span>{user?.username ?? '未登录'}</span>
            </Space>
          </Dropdown>
        </Header>
        <Content className="admin-content">
          {children}
        </Content>
      </Layout>
    </Layout>
  );
}

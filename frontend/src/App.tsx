import React from 'react';
import { Navigate, Outlet, Route, Routes } from 'react-router-dom';
import { RequireAuth } from './auth/RequireAuth';
import { useAuth } from './auth/AuthContext';
import { AdminLayout } from './layouts/AdminLayout';
import { LoginPage } from './pages/Login';
import { DevicesPage } from './pages/Devices';
import { GroupsPage } from './pages/Groups';
import { UserGroupsPage } from './pages/UserGroups';
import { UsersPage } from './pages/Users';

function AdminShell() {
  return (
    <RequireAuth>
      <AdminLayout>
        <Outlet />
      </AdminLayout>
    </RequireAuth>
  );
}

function HomeRedirect() {
  const { user } = useAuth();
  const perms = new Set(user?.permissions ?? []);
  if (user?.superAdmin || perms.has('device:list')) {
    return <Navigate to="/devices" replace />;
  }
  if (perms.has('group:list')) {
    return <Navigate to="/groups" replace />;
  }
  if (perms.has('usergroup:list')) {
    return <Navigate to="/user-groups" replace />;
  }
  if (perms.has('user:list')) {
    return <Navigate to="/users" replace />;
  }
  return <Navigate to="/login" replace />;
}

export default function App(): JSX.Element {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<AdminShell />}>
        <Route path="/" element={<HomeRedirect />} />
        <Route path="/devices" element={<DevicesPage />} />
        <Route path="/groups" element={<GroupsPage />} />
        <Route path="/user-groups" element={<UserGroupsPage />} />
        <Route path="/users" element={<UsersPage />} />
        <Route path="*" element={<HomeRedirect />} />
      </Route>
    </Routes>
  );
}

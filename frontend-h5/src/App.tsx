import { BrowserRouter, Navigate, Route, Routes, Outlet, useNavigate } from 'react-router-dom';
import { Button, Space, Typography } from 'antd';
import { AuthProvider, useAuth } from './auth/AuthContext';
import { RequireAuth } from './auth/RequireAuth';
import { LoginPage } from './pages/Login';
import { GroupsPage } from './pages/Groups';
import { GroupDetailPage } from './pages/GroupDetail';

const { Text } = Typography;

function AppLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="h5-layout">
      <div className="h5-header">
        <div className="h5-title">设备控制</div>
        <Space size={8}>
          <Text>{user?.username ?? '用户'}</Text>
          <Button size="small" onClick={handleLogout}>退出</Button>
        </Space>
      </div>
      <div className="h5-content">
        <Outlet />
      </div>
    </div>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route
            element={(
              <RequireAuth>
                <AppLayout />
              </RequireAuth>
            )}
          >
            <Route index element={<GroupsPage />} />
            <Route path="groups/:id" element={<GroupDetailPage />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Route>
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}

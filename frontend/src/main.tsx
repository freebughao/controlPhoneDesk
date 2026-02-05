import React from 'react';
import ReactDOM from 'react-dom/client';
import { Buffer } from 'buffer';
import { BrowserRouter } from 'react-router-dom';
import App from './App';
import { AuthProvider } from './auth/AuthContext';
import 'antd/dist/reset.css';
import './styles.css';

(window as unknown as { Buffer?: typeof Buffer }).Buffer = Buffer;

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <App />
      </AuthProvider>
    </BrowserRouter>
  </React.StrictMode>
);

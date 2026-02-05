import React from 'react';
import ReactDOM from 'react-dom/client';
import { Buffer } from 'buffer';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import App from './App';
import 'antd/dist/reset.css';
import './styles.css';

// Provide Buffer polyfill for legacy scrcpy utilities.
(window as unknown as { Buffer?: typeof Buffer }).Buffer = Buffer;

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ConfigProvider locale={zhCN}>
      <App />
    </ConfigProvider>
  </React.StrictMode>
);

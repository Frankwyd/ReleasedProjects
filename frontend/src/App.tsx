import React from 'react';
import { BrowserRouter as Router, Routes, Route, Link } from 'react-router-dom';
import { Layout, Menu } from 'antd';
import TradeTable from './components/TradeTable';
import MarketData from './components/MarketData';
import Ore from './pages/Ore';
import './App.css';

const { Header, Content } = Layout;

const App: React.FC = () => {
  const menuItems = [
    { key: '1', label: <Link to="/">交易明细</Link> },
    { key: '2', label: <Link to="/market-data">市场数据</Link> },
    { key: '3', label: <Link to="/ore">Ore</Link> }
  ];

  return (
    <Router>
      <Layout>
        <Header>
          <Menu theme="dark" mode="horizontal" items={menuItems} />
        </Header>
        <Content style={{ padding: '20px' }}>
          <Routes>
            <Route path="/" element={<TradeTable />} />
            <Route path="/market-data" element={<MarketData />} />
            <Route path="/ore" element={<Ore />} />
          </Routes>
        </Content>
      </Layout>
    </Router>
  );
};

export default App; 
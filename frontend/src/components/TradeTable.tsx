import '../utils/global-polyfill';
import React, { useEffect, useState } from 'react';
import { Table, message, Button } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import axios from 'axios';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

interface Trade {
  'Trade Date': string;
  'Valuation Date': string;
  'Book': string;
  'BBG ID': string;
  'Calypso ID': string;
  'Strategy': string;
  'Undl': string;
  'Notional': number;
  'Cpty': string;
  'Product Type': string;
  'Asset Class': string;
  'OptMult': number;
  'Expiry Date': string;
  'Settl. Date': string;
  'Strike': string;
  'Cutoff': number;
}

const sortTradesByDate = (trades: Trade[]) => {
  return [...trades].sort((a, b) => {
    const timeA = new Date(a['Trade Date'].replace(/(\d{4})(\d{2})(\d{2}) (\d{2}):(\d{2}):(\d{2})/, '$1-$2-$3T$4:$5:$6')).getTime();
    const timeB = new Date(b['Trade Date'].replace(/(\d{4})(\d{2})(\d{2}) (\d{2}):(\d{2}):(\d{2})/, '$1-$2-$3T$4:$5:$6')).getTime();
    return timeB - timeA;  // 倒序排列
  });
};

// 计算上一个工作日的函数
const getPreviousWorkDay = (date = new Date()) => {
  const previousDay = new Date(date);
  do {
    previousDay.setDate(previousDay.getDate() - 1);
  } while (previousDay.getDay() === 0 || previousDay.getDay() === 6);
  
  return previousDay.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  }).replace(/\//g, '-');
};

const TradeTable: React.FC = () => {
  const [trades, setTrades] = useState<Trade[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [connected, setConnected] = useState(false);
  const [currentDate] = useState(new Date().toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  }).replace(/\//g, '-'));
  const [previousWorkDay] = useState(getPreviousWorkDay());

  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      debug: function (str) {
        console.log('STOMP: ' + str);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    client.onConnect = () => {
      console.log('Connected to WebSocket');
      setConnected(true);
      setError(null);

      client.subscribe('/topic/trades', (message) => {
        try {
          const response = JSON.parse(message.body);
          console.log('Received WebSocket message:', response);
          
          if (response.status === 'success' || response.status === 'no_update') {
            if (Array.isArray(response.data)) {
              const sortedTrades = sortTradesByDate(response.data);
              setTrades(sortedTrades);
              console.log('Updated trades data:', sortedTrades);
            } else {
              console.warn('Received data is not an array:', response.data);
            }
          } else {
            console.warn('Received message with unexpected status:', response.status);
          }
        } catch (e) {
          console.error('Error processing WebSocket message:', e);
        }
      });

      // 初始加载数据
      fetchTrades();
    };

    client.onStompError = (frame) => {
      console.error('STOMP error:', frame);
      setError('WebSocket 连接错误');
      setConnected(false);
    };

    client.onWebSocketClose = () => {
      console.log('WebSocket connection closed');
      setConnected(false);
    };

    client.activate();

    return () => {
      client.deactivate();
    };
  }, []);

  const fetchTrades = async () => {
    try {
      setLoading(true);
      const response = await axios.get('http://localhost:8080/api/trades');
      if (response.data.status === 'success' || response.data.status === 'no_update') {
        if (Array.isArray(response.data.data)) {
          const sortedTrades = sortTradesByDate(response.data.data);
          setTrades(sortedTrades);
        }
      }
    } catch (error) {
      console.error('Error fetching trades:', error);
      message.error('获取数据失败');
    } finally {
      setLoading(false);
    }
  };

  const handleRefresh = async () => {
    try {
      const response = await axios.get('http://localhost:8080/api/trades/refresh');
      if (response.data.status === 'success' || response.data.status === 'no_update') {
        if (Array.isArray(response.data.data)) {
          const sortedTrades = sortTradesByDate(response.data.data);
          setTrades(sortedTrades);
        }
      } else {
        message.error('Failed to refresh trades: ' + response.data.message);
      }
    } catch (error) {
      message.error('Error refreshing trades: ' + error.message);
    }
  };

  const columns: ColumnsType<Trade> = [
    {
      title: '交易日期',
      dataIndex: 'Trade Date',
      key: 'tradeDate',
      render: (value: string) => {
        const match = value.match(/(\d{4})(\d{2})(\d{2})(?: (\d{2}):(\d{2}):(\d{2}))?/);
        if (match) {
          const [, year, month, day, hour, minute, second] = match;
          if (hour && minute && second) {
            return `${year}-${month}-${day} ${hour}:${minute}:${second}`;
          }
          return `${year}-${month}-${day}`;
        }
        return value;
      },
    },
    {
      title: '定价日期',
      dataIndex: 'Valuation Date',
      key: 'valuationDate',
      render: (value: string) => value.replace(/(\d{4})(\d{2})(\d{2})/, '$1-$2-$3'),
    },
    {
      title: '交易簿',
      dataIndex: 'Book',
      key: 'book',
    },
    {
      title: '标的',
      dataIndex: 'Undl',
      key: 'undl',
    },
    {
      title: '名义金额',
      dataIndex: 'Notional',
      key: 'notional',
      align: 'right',
      render: (value: number) => value.toLocaleString(),
    },
    {
      title: '交易对手',
      dataIndex: 'Cpty',
      key: 'cpty',
    },
    {
      title: '产品类型',
      dataIndex: 'Product Type',
      key: 'productType',
    },
    {
      title: '资产类别',
      dataIndex: 'Asset Class',
      key: 'assetClass',
    },
    {
      title: '结算日期',
      dataIndex: 'Settl. Date',
      key: 'settlementDate',
      render: (value: string) => value === '########' ? '-' : value.replace(/(\d{4})(\d{2})(\d{2})/, '$1-$2-$3'),
    },
    {
      title: '截止价',
      dataIndex: 'Cutoff',
      key: 'cutoff',
      align: 'right',
      render: (value: number) => value.toFixed(4),
    },
  ];

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
        <h2>交易明细</h2>
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
          <div style={{ 
            display: 'flex', 
            alignItems: 'center', 
            gap: '8px',
            padding: '4px 12px',
            background: '#f5f5f5',
            borderRadius: '4px'
          }}>
            <span>当前日期：{currentDate}</span>
            <span style={{ color: '#8c8c8c' }}>|</span>
            <span>上一工作日：{previousWorkDay}</span>
          </div>
          <span style={{ 
            color: connected ? '#52c41a' : '#ff4d4f' 
          }}>
            {connected ? '已连接' : '未连接'}
          </span>
          <Button onClick={handleRefresh}>
            获取最新交易集
          </Button>
        </div>
      </div>

      {error && (
        <div style={{ 
          padding: '12px', 
          marginBottom: '16px', 
          background: '#fff2f0', 
          border: '1px solid #ffccc7',
          borderRadius: '4px'
        }}>
          {error}
        </div>
      )}

      <Table<Trade>
        dataSource={trades}
        columns={columns}
        loading={loading}
        rowKey="Calypso ID"
        pagination={{
          pageSize: 10,
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (total) => `共 ${total} 条交易`,
          pageSizeOptions: ['10', '20', '50', '100'],
          position: ['bottomLeft'],
          size: 'default',
          style: { marginTop: '16px' }
        }}
        scroll={{ x: 'max-content' }}
        size="middle"
        bordered
      />
    </div>
  );
};

export default TradeTable; 
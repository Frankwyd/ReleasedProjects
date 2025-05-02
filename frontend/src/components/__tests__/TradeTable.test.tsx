import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import TradeTable from '../TradeTable';
import { Client } from '@stomp/stompjs';

// Mock WebSocket
jest.mock('@stomp/stompjs', () => ({
  Client: jest.fn().mockImplementation(() => ({
    activate: jest.fn(),
    deactivate: jest.fn(),
    onConnect: jest.fn(),
    onStompError: jest.fn(),
    subscribe: jest.fn(),
  })),
}));

describe('TradeTable', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('renders trade table with data', async () => {
    const mockTrades = [
      {
        'Trade Date': '20250212 14:30:00',
        'Valuation Date': '20250212',
        'Book': 'FXOPT',
        'Undl': 'USDJPY',
        'Notional': 20000000,
        'Cpty': 'HK-GSIG',
        'Product Type': 'FX Spot',
        'Asset Class': 'FX',
        'Cutoff': 149.2500
      }
    ];

    render(<TradeTable />);

    // 验证表格标题
    expect(screen.getByText('交易明细')).toBeInTheDocument();

    // 模拟WebSocket消息
    const mockClient = Client as jest.Mock;
    const mockSubscribe = mockClient.mock.results[0].value.subscribe;
    mockSubscribe.mockImplementation((topic: string, callback: Function) => {
      callback({
        body: JSON.stringify({
          status: 'success',
          data: mockTrades
        })
      });
    });

    // 等待数据加载
    await waitFor(() => {
      expect(screen.getByText('2025-02-12 14:30:00')).toBeInTheDocument();
      expect(screen.getByText('USDJPY')).toBeInTheDocument();
      expect(screen.getByText('20,000,000')).toBeInTheDocument();
    });
  });

  test('handles WebSocket connection error', async () => {
    render(<TradeTable />);

    const mockClient = Client as jest.Mock;
    const mockOnStompError = mockClient.mock.results[0].value.onStompError;
    mockOnStompError.mockImplementation((callback: Function) => {
      callback({ body: 'Connection error' });
    });

    await waitFor(() => {
      expect(screen.getByText(/WebSocket 连接错误/)).toBeInTheDocument();
    });
  });
}); 
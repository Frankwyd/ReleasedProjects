import React, { useState, useEffect } from 'react';
import { Button, message, Input, Form, DatePicker, Switch, Space, Card, Select } from 'antd';
import dayjs, { Dayjs } from 'dayjs';
import axios from 'axios';

// 添加自定义样式
const formItemStyle = {
    '.ant-form-item': {
        marginBottom: '0 !important',
    },
    '.ant-form-item-label': {
        paddingBottom: '0 !important',
        lineHeight: '1 !important',
    },
    '.ant-form-item-control': {
        marginTop: '0 !important',
        lineHeight: '1 !important',
    },
    '.ant-form-item-control-input': {
        minHeight: '32px !important',
    },
    '.ant-form-item-control-input-content': {
        lineHeight: '1 !important',
    }
};

const MARKET_DATA_PATH = 'E:\\CursorAI\\MarketData';
const DEFAULT_LIVE_WORKING_DIR = 'E:\\CursorAI\\OreBatches\\Live';
const DEFAULT_EOD_WORKING_DIR = 'E:\\CursorAI\\OreBatches\\EOD';
const DEFAULT_SOD_WORKING_DIR = 'E:\\CursorAI\\OreBatches\\Tp1SOD';

interface ApiResponse {
  status: string;
  file: string;
  message?: string;
}

const OreBatch: React.FC = () => {
    // ... 保持原有代码不变 ...
};

export default OreBatch; 
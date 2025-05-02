import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { DatePicker, Switch, Card, Space, Table, Button, message, AutoComplete } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs, { Dayjs } from 'dayjs';
import axios from 'axios';

// 计算上一个工作日的函数
const getPreviousWorkDay = (date: Dayjs = dayjs()): Dayjs => {
  let previousDay = date.subtract(1, 'day');
  while (previousDay.day() === 0 || previousDay.day() === 6) {
    previousDay = previousDay.subtract(1, 'day');
  }
  return previousDay;
};

// 生成市场数据文件名
const getMarketDataFilename = (date: Dayjs, isLive: boolean, isTM1: boolean): string => {
  if (isTM1) {
    return `IRFX Market_${date.format('YYYYMMDD')}_Close.xls`;
  }
  return isLive ? 'IRFX Market_YYYYMMDD_Close.xlsIRFX Market_YYYYMMDD_Close.xlsIRFX Market_Live.xls' : `IRFX Market_${date.format('YYYYMMDD')}_Close.xls`;
};

// 定义市场数据接口
interface MarketDataItem {
  key: string;
  bbgTicker: string;
  tm1EOD: string;
  currentValue: string;
  diff: string;
}

// 定义后端返回的数据接口
interface MarketDataResponse {
  ticker: string;
  tm1Value: string;
  currentValue: string;
  diff: string;
}

const MarketData: React.FC = () => {
  const [isLive, setIsLive] = useState(true);
  const [selectedDate, setSelectedDate] = useState<Dayjs>(dayjs());
  const [previousWorkDay, setPreviousWorkDay] = useState<Dayjs>(getPreviousWorkDay());
  const [loading, setLoading] = useState(false);
  const [marketData, setMarketData] = useState<MarketDataItem[]>([]);
  const [searchValue, setSearchValue] = useState('');
  const [filteredData, setFilteredData] = useState<MarketDataItem | null>(null);
  const [lastLoadTime, setLastLoadTime] = useState<string>('');
  const [dataCache, setDataCache] = useState<{[key: string]: MarketDataItem[]}>({});
  const [searchTimeout, setSearchTimeout] = useState<number>();

  // 生成缓存键
  const getCacheKey = useCallback((tm1File: string, currentFile: string) => {
    return `${tm1File}|${currentFile}`;
  }, []);

  // 使用 useMemo 优化自动完成选项的计算
  const autoCompleteOptions = useMemo(() => {
    if (!searchValue) return [];
    const lowerSearchValue = searchValue.toLowerCase();
    return marketData
      .filter(item => item.bbgTicker.toLowerCase().includes(lowerSearchValue))
      .map(item => ({
        value: item.bbgTicker,
        label: item.bbgTicker,
      }));
  }, [searchValue, marketData]);

  // 处理搜索
  const handleSearch = useCallback((value: string) => {
    setSearchValue(value);
    
    // 清除之前的定时器
    if (searchTimeout) {
      window.clearTimeout(searchTimeout);
    }

    // 设置新的定时器
    const timeoutId = window.setTimeout(() => {
      if (!value) {
        setFilteredData(null);
        return;
      }

      const matchedData = marketData.find(
        item => item.bbgTicker.toLowerCase() === value.toLowerCase()
      );
      setFilteredData(matchedData || null);
    }, 300);

    setSearchTimeout(timeoutId);
  }, [marketData, searchTimeout]);

  // 组件卸载时清理定时器
  useEffect(() => {
    return () => {
      if (searchTimeout) {
        window.clearTimeout(searchTimeout);
      }
    };
  }, [searchTimeout]);

  // 在 Live 模式下每秒更新当前时间
  useEffect(() => {
    let timerId: ReturnType<typeof setInterval>;
    if (isLive) {
      timerId = setInterval(() => {
        const now = dayjs();
        setSelectedDate(now);
        setPreviousWorkDay(getPreviousWorkDay(now));
      }, 1000);
    }
    return () => {
      if (timerId) {
        clearInterval(timerId);
      }
    };
  }, [isLive]);

  const handleModeChange = (checked: boolean) => {
    setIsLive(checked);
    // 当切换到Live模式时，重置日期为当前日期
    if (checked) {
      const now = dayjs();
      setSelectedDate(now);
      setPreviousWorkDay(getPreviousWorkDay(now));
    }
  };

  const handleDateChange = (date: Dayjs | null) => {
    if (date) {
      setSelectedDate(date);
      setPreviousWorkDay(getPreviousWorkDay(date));
    }
  };

  const baseDataPath = 'E:\\CursorAI\\MarketData';
  const tm1Filename = getMarketDataFilename(previousWorkDay, false, true);
  const tFilename = getMarketDataFilename(selectedDate, isLive, false);

  const handleLoadData = async () => {
    try {
      const cacheKey = getCacheKey(tm1Filename, tFilename);
      const currentTime = dayjs().format('YYYY-MM-DD HH:mm:ss');

      // 检查缓存是否存在且在5分钟内
      if (dataCache[cacheKey] && 
          lastLoadTime && 
          dayjs().diff(dayjs(lastLoadTime), 'minute') < 5 &&
          !isLive) {
        setMarketData(dataCache[cacheKey]);
        message.success('从缓存加载数据成功');
        return;
      }

      setLoading(true);
      const response = await axios.post('http://localhost:8080/api/market-data/load', {
        basePath: baseDataPath,
        tm1File: tm1Filename,
        currentFile: tFilename
      }, {
        timeout: 30000,
        headers: {
          'Accept-Encoding': 'gzip, deflate'
        }
      });

      if (response.data.status === 'success' && Array.isArray(response.data.data)) {
        const newData = response.data.data.map((item: MarketDataResponse, index: number) => ({
          key: index.toString(),
          bbgTicker: item.ticker,
          tm1EOD: item.tm1Value,
          currentValue: item.currentValue,
          diff: item.diff
        }));
        setMarketData(newData);
        
        // 更新缓存
        if (!isLive) {
          setDataCache(prev => ({
            ...prev,
            [cacheKey]: newData
          }));
          setLastLoadTime(currentTime);
        }
        
        message.success('数据加载成功');
      } else {
        message.error('数据加载失败：' + (response.data.message || '未知错误'));
      }
    } catch (error) {
      message.error('数据加载失败：' + (error instanceof Error ? error.message : '未知错误'));
    } finally {
      setLoading(false);
    }
  };

  const handleConvertToOre = async () => {
    try {
      setLoading(true);
      const response = await axios.post('http://localhost:8080/api/market-data/convert-to-ore', {
        basePath: baseDataPath,
        tm1File: tm1Filename,
        date: selectedDate.format('YYYY-MM-DD')
      });
      
      if (response.data.status === 'success') {
        message.success('数据已成功转换为ORE格式');
      } else {
        message.error('转换失败：' + response.data.message);
      }
    } catch (error) {
      message.error('转换失败：' + (error instanceof Error ? error.message : '未知错误'));
    } finally {
      setLoading(false);
    }
  };

  // 定义表格列
  const columns: ColumnsType<MarketDataItem> = [
    {
      title: 'BBG Ticker',
      dataIndex: 'bbgTicker',
      key: 'bbgTicker',
      width: '30%',
    },
    {
      title: 'TM1 EOD',
      dataIndex: 'tm1EOD',
      key: 'tm1EOD',
      align: 'right',
      render: (value: string) => {
        if (value === 'ERR' || value === 'N/A') {
          return <span style={{ color: '#666666' }}>{value}</span>;
        }
        return value;
      }
    },
    {
      title: isLive ? 'Live' : 'T EOD',
      dataIndex: 'currentValue',
      key: 'currentValue',
      align: 'right',
      render: (value: string) => {
        if (value === 'ERR' || value === 'N/A') {
          return <span style={{ color: '#666666' }}>{value}</span>;
        }
        return value;
      }
    },
    {
      title: 'Diff',
      dataIndex: 'diff',
      key: 'diff',
      align: 'right',
      render: (value: string) => {
        if (value === 'ERR' || value === 'N/A') {
          return <span style={{ color: '#666666' }}>{value}</span>;
        }
        const numValue = parseFloat(value);
        const color = numValue > 0 ? '#52c41a' : numValue < 0 ? '#ff4d4f' : 'inherit';
        return <span style={{ color }}>{value}</span>;
      },
    },
  ];

  return (
    <div style={{ padding: '24px' }}>
      {/* 右上角信息栏 */}
      <div style={{
        position: 'absolute',
        top: '24px',
        right: '24px',
        display: 'flex',
        flexDirection: 'column',
        gap: '8px',
        background: '#f5f5f5',
        padding: '12px 16px',
        borderRadius: '4px',
        boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
        maxWidth: '600px',
        zIndex: 1
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span>当前日期：{selectedDate.format('YYYY-MM-DD')}</span>
          <span style={{ color: '#8c8c8c' }}>|</span>
          <span>上一工作日：{previousWorkDay.format('YYYY-MM-DD')}</span>
        </div>
        <div style={{ fontSize: '13px', color: '#666' }}>
          <div>数据路径：{baseDataPath}</div>
          <div>T日文件：{tFilename}</div>
          <div>TM1日文件：{tm1Filename}</div>
        </div>
      </div>

      <Card 
        title="市场数据" 
        bordered={false}
        style={{ maxWidth: '1000px', margin: '0 auto' }}
      >
        <Space direction="vertical" size="large" style={{ width: '100%' }}>
          <Space size="large">
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <span>模式：</span>
              <Switch
                checkedChildren="Live"
                unCheckedChildren="EOD"
                checked={isLive}
                onChange={handleModeChange}
                style={{ minWidth: '70px' }}
              />
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <span>日期：</span>
              <DatePicker
                value={selectedDate}
                onChange={handleDateChange}
                disabled={isLive}
                format="YYYY-MM-DD"
                allowClear={false}
                style={{ width: '150px' }}
              />
            </div>
            <Button 
              type="primary"
              onClick={handleLoadData}
              loading={loading}
              icon={<span className="anticon">📊</span>}
            >
              加载市场数据
            </Button>
            <Button 
              type="primary"
              onClick={handleConvertToOre}
              loading={loading}
              icon={<span className="anticon">🔄</span>}
            >
              转换为ORE格式
            </Button>
          </Space>

          {/* BBG Ticker筛选框和结果显示 */}
          <div style={{ 
            display: 'flex', 
            gap: '16px', 
            alignItems: 'stretch',
            marginBottom: '16px',
            height: '56px'  // 固定高度以保持一致
          }}>
            <div style={{ 
              width: '30%',  // 与表格中BBG Ticker列宽度保持一致
              display: 'flex',
              alignItems: 'center'
            }}>
              <AutoComplete
                value={searchValue}
                options={autoCompleteOptions}
                onChange={handleSearch}
                onSelect={handleSearch}
                placeholder="输入BBG Ticker进行筛选"
                style={{ width: '100%' }}
              />
            </div>
            <div style={{ 
              flex: 1,
              display: 'flex',
              alignItems: 'center'
            }}>
              {filteredData ? (
                <div style={{ 
                  display: 'grid', 
                  gridTemplateColumns: 'repeat(3, 1fr)', 
                  gap: '16px',
                  width: '100%',
                  background: '#f5f5f5',
                  padding: '12px 16px',
                  borderRadius: '4px',
                }}>
                  <div>
                    <div style={{ color: '#666', fontSize: '12px', marginBottom: '4px' }}>TM1 EOD</div>
                    <div style={{ 
                      fontSize: '14px',
                      fontWeight: 500,
                      color: filteredData.tm1EOD === 'ERR' || filteredData.tm1EOD === 'N/A' 
                        ? '#666666' 
                        : 'inherit',
                      textAlign: 'right'
                    }}>
                      {filteredData.tm1EOD}
                    </div>
                  </div>
                  <div>
                    <div style={{ color: '#666', fontSize: '12px', marginBottom: '4px' }}>
                      {isLive ? 'Live' : 'T EOD'}
                    </div>
                    <div style={{ 
                      fontSize: '14px',
                      fontWeight: 500,
                      color: filteredData.currentValue === 'ERR' || filteredData.currentValue === 'N/A'
                        ? '#666666'
                        : 'inherit',
                      textAlign: 'right'
                    }}>
                      {filteredData.currentValue}
                    </div>
                  </div>
                  <div>
                    <div style={{ color: '#666', fontSize: '12px', marginBottom: '4px' }}>Diff</div>
                    <div style={{ 
                      fontSize: '14px',
                      fontWeight: 500,
                      color: filteredData.diff === 'ERR' || filteredData.diff === 'N/A'
                        ? '#666666'
                        : parseFloat(filteredData.diff) > 0
                        ? '#52c41a'
                        : parseFloat(filteredData.diff) < 0
                        ? '#ff4d4f'
                        : 'inherit',
                      textAlign: 'right'
                    }}>
                      {filteredData.diff}
                    </div>
                  </div>
                </div>
              ) : (
                <div style={{ 
                  width: '100%',
                  background: '#f5f5f5',
                  padding: '12px 16px',
                  borderRadius: '4px',
                  color: '#666',
                  textAlign: 'center',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                }}>
                  请输入BBG Ticker查看详细数据
                </div>
              )}
            </div>
          </div>

          <Table<MarketDataItem>
            columns={columns}
            dataSource={marketData}
            pagination={{
              pageSize: 50,
              showSizeChanger: true,
              showQuickJumper: true
            }}
            size="middle"
            bordered
            loading={loading}
            scroll={{ y: 400 }}  // 启用虚拟滚动
          />
        </Space>
      </Card>
    </div>
  );
};

export default MarketData; 
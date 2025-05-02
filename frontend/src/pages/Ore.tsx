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

const Ore: React.FC = () => {
    const [loading, setLoading] = useState(false);
    const [preparingFiles, setPreparingFiles] = useState(false);
    const [result, setResult] = useState<string | null>(null);
    const [form] = Form.useForm();
    const [isLive, setIsLive] = useState(true);
    const [selectedDate, setSelectedDate] = useState<Dayjs>(dayjs());
    const [lastLoadTime, setLastLoadTime] = useState<string>('');
    const [marketFiles, setMarketFiles] = useState<string[]>([]);
    const [selectedMarketFile, setSelectedMarketFile] = useState<string>('IRFX Market_Live.xls');
    const API_BASE_URL = 'http://localhost:8080';
    const [updateInfo, setUpdateInfo] = useState<string>('');
    const [errorMessage, setErrorMessage] = useState<string>('');

    // 添加自定义样式
    const switchStyle = {
        '.ant-switch-checked': {
            backgroundColor: '#ff4d4f !important', // Live模式下的红色
        },
        '.ant-switch': {
            backgroundColor: '#1890ff !important', // EOD模式下的蓝色
        }
    };

    // 初始化表单值
    useEffect(() => {
        form.setFieldsValue({
            workingDir: DEFAULT_LIVE_WORKING_DIR,
            sodWorkingDir: undefined
        });
    }, []);

    // 当模式改变时更新工作目录
    useEffect(() => {
        if (isLive) {
            form.setFieldsValue({
                workingDir: DEFAULT_LIVE_WORKING_DIR,
                sodWorkingDir: undefined
            });
        } else {
            form.setFieldsValue({
                workingDir: DEFAULT_EOD_WORKING_DIR,
                sodWorkingDir: DEFAULT_SOD_WORKING_DIR
            });
        }
    }, [isLive, form]);

    // 获取市场数据文件列表
    const fetchMarketFiles = async () => {
        try {
            const currentDate = selectedDate.format('YYYYMMDD');
            console.log('Fetching market files for date:', currentDate);
            const response = await axios.get(`${API_BASE_URL}/api/ore/market-files`, {
                params: {
                    path: MARKET_DATA_PATH,
                    date: currentDate
                }
            });
            console.log('Market files response:', response.data);
            if (response.data.status === 'success') {
                const files = response.data.files;
                console.log('Received files:', files);
                
                // 根据模式过滤文件
                let filteredFiles;
                if (isLive) {
                    // Live模式下显示所有文件
                    filteredFiles = files;
                    // 确保默认选中Live文件
                    if (!filteredFiles.includes('IRFX Market_Live.xls')) {
                        filteredFiles.unshift('IRFX Market_Live.xls');
                    }
                } else {
                    // EOD模式下只显示特定格式的文件
                    const expectedFileName = `IRFX Market_${currentDate}_Close.xls`;
                    filteredFiles = files.filter((file: string) => file === expectedFileName);
                    
                    // 如果文件不存在，显示提示信息
                    if (filteredFiles.length === 0) {
                        message.warning(`市场数据文件 ${expectedFileName} 不存在`);
                        // 添加一个提示选项
                        filteredFiles = [`市场数据文件 ${expectedFileName} 不存在`];
                        setSelectedMarketFile(filteredFiles[0]);
                    }
                }
                
                console.log('Filtered files:', filteredFiles);
                setMarketFiles(filteredFiles);
                
                // 如果当前没有选中文件，或者选中的文件不在过滤后的列表中，则选择第一个文件
                if (!selectedMarketFile || !filteredFiles.includes(selectedMarketFile)) {
                    const defaultFile = filteredFiles[0] || (isLive ? 'IRFX Market_Live.xls' : '');
                    console.log('Setting default file to:', defaultFile);
                    setSelectedMarketFile(defaultFile);
                }
            }
        } catch (error) {
            console.error('Failed to fetch market files:', error);
            message.error('Failed to fetch market files');
        }
    };

    // 在Live模式下每秒更新当前时间
    useEffect(() => {
        let timerId: ReturnType<typeof setInterval>;
        if (isLive) {
            // 初始加载文件列表
            fetchMarketFiles();
            
            // 每秒更新当前时间，但只在整分钟时刷新文件列表
            timerId = setInterval(() => {
                const now = dayjs();
                setSelectedDate(now);
                
                // 只在整分钟时刷新文件列表
                if (now.second() === 0) {
                    fetchMarketFiles();
                }
            }, 1000);
        }
        return () => {
            if (timerId) {
                clearInterval(timerId);
            }
        };
    }, [isLive]); // 只在isLive改变时重新设置定时器

    // 当日期改变时更新文件列表（仅在EOD模式下）
    useEffect(() => {
        if (!isLive) {
            fetchMarketFiles();
        }
    }, [selectedDate, isLive]);

    const handleModeChange = (checked: boolean) => {
        setIsLive(checked);
        if (checked) {
            // 切换到Live模式时
            setSelectedDate(dayjs());
            setSelectedMarketFile('IRFX Market_Live.xls');
            // 重置市场数据文件列表
            setMarketFiles([]);
            // 立即获取最新的市场数据文件
            fetchMarketFiles();
            // 更新工作目录
            form.setFieldsValue({
                workingDir: DEFAULT_LIVE_WORKING_DIR,
                sodWorkingDir: undefined
            });
        } else {
            // 切换到EOD模式时
            form.setFieldsValue({
                workingDir: DEFAULT_EOD_WORKING_DIR,
                sodWorkingDir: DEFAULT_SOD_WORKING_DIR
            });
            // 只在切换时获取一次文件列表
            fetchMarketFiles();
        }
    };

    const handleDateChange = (date: Dayjs | null) => {
        if (date) {
            setSelectedDate(date);
            // 在EOD模式下，日期改变时刷新文件列表
            if (!isLive) {
                fetchMarketFiles();
            }
        }
    };

    const handleMarketFileChange = (value: string) => {
        setSelectedMarketFile(value);
    };

    const handleDropdownVisibleChange = (open: boolean) => {
        if (open) {
            fetchMarketFiles();
        }
    };

    const handleTest = async () => {
        try {
            console.log('Sending test request...');
            const response = await fetch(`${API_BASE_URL}/api/ore/test`);
            const data = await response.text();
            console.log('Test response:', data);
            message.success('Test successful: ' + data);
        } catch (error) {
            console.error('Test failed:', error);
            message.error('Test failed: ' + error);
        }
    };

    // 获取下一个工作日
    const getNextBusinessDay = (date: Dayjs): Dayjs => {
        let nextDay = date.add(1, 'day');
        // 如果是周六，加2天到周一
        if (nextDay.day() === 6) {
            nextDay = nextDay.add(2, 'day');
        }
        // 如果是周日，加1天到周一
        else if (nextDay.day() === 0) {
            nextDay = nextDay.add(1, 'day');
        }
        return nextDay;
    };

    const handleRunOre = async () => {
        console.log('Starting ORE execution...');
        setLoading(true);
        setResult(''); // 清空之前的结果

        const formatLine = (line: string) => {
            if (line.includes('Loading inputs') || 
                line.includes('Requested analytics') || 
                line.includes('Pricing:')) {
                return `- ${line.trim()}`;
            } else if (line.includes('run time:')) {
                return `运行时间: ${line.split('run time:')[1].trim()} 秒`;
            } else if (line.includes('ORE done')) {
                return '状态: ORE done.';
            }
            return line;
        };

        try {
            const values = await form.validateFields();
            const workingDir = values.workingDir;
            const sodWorkingDir = values.sodWorkingDir;
            const currentTime = dayjs().format('YYYY-MM-DD HH:mm:ss');
            setLastLoadTime(currentTime);
            
            if (isLive) {
                // Live模式：清理output目录后运行
                setResult('正在清理 Live Batch 输出目录...\n');
                console.log('Cleaning output directory for Live Batch...');
                const cleanResponse = await fetch(`${API_BASE_URL}/api/ore/clean-directory`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        directory: workingDir
                    })
                });

                if (!cleanResponse.ok) {
                    throw new Error(`Failed to clean output directory: ${cleanResponse.status}`);
                }
                setResult(prev => prev + '输出目录清理完成\n\n');

                // 运行ORE
                setResult(prev => prev + '开始运行 Live Batch...\n');
                console.log('Sending request to /api/ore/run with working directory:', workingDir);
                const response = await fetch(`${API_BASE_URL}/api/ore/run`, { 
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        workingDir,
                        date: selectedDate.format('YYYY-MM-DD'),
                        isLive,
                        marketFile: selectedMarketFile,
                        marketDataPath: MARKET_DATA_PATH
                    })
                });
                
                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }

                // 使用 ReadableStream 处理响应
                const reader = response.body?.getReader();
                if (!reader) {
                    throw new Error('Response body is not readable');
                }

                let buffer = '';
                const decoder = new TextDecoder();

                while (true) {
                    const { done, value } = await reader.read();
                    if (done) break;
                    
                    // 将新接收的数据添加到缓冲区
                    buffer += decoder.decode(value, { stream: true });
                    
                    // 查找完整的行
                    const lines = buffer.split('\n');
                    // 保留最后一个可能不完整的行
                    buffer = lines.pop() || '';
                    
                    // 处理完整的行
                    if (lines.length > 0) {
                        setResult(prev => {
                            const newLines = lines.map(formatLine).join('\n');
                            return prev + newLines + '\n';
                        });
                    }
                }

                // 处理剩余的缓冲区
                if (buffer) {
                    setResult(prev => prev + formatLine(buffer) + '\n');
                }

                // 保存输出文件到指定目录
                setResult(prev => prev + '\n正在保存输出文件...\n');
                console.log('Saving output files...');
                const saveResponse = await fetch(`${API_BASE_URL}/api/ore/save-output`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        sourceDir: workingDir,
                        targetDir: 'E:\\CursorAI\\OreResults\\LiveBatch'
                    })
                });

                if (!saveResponse.ok) {
                    throw new Error(`Failed to save output files: ${saveResponse.status}`);
                }

                const saveResult = await saveResponse.text();
                console.log('Save output result:', saveResult);
                setResult(prev => prev + '输出文件保存完成\n');
                message.success('Ore execution completed and output files saved');
            } else {
                // EOD模式：依次在EOD Batch和SOD Batch目录运行
                // 1. 清理工作目录
                setResult('正在清理 EOD/SOD Batch 输出目录...\n');
                console.log('Cleaning working directories...');
                const cleanEodResponse = await fetch(`${API_BASE_URL}/api/ore/clean-directory`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        directory: workingDir
                    })
                });

                if (!cleanEodResponse.ok) {
                    throw new Error(`Failed to clean EOD directory: ${cleanEodResponse.status}`);
                }

                const cleanSodResponse = await fetch(`${API_BASE_URL}/api/ore/clean-directory`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        directory: sodWorkingDir
                    })
                });

                if (!cleanSodResponse.ok) {
                    throw new Error(`Failed to clean SOD directory: ${cleanSodResponse.status}`);
                }
                setResult(prev => prev + '输出目录清理完成\n\n');

                // 2. 运行EOD Batch
                setResult(prev => prev + '=== EOD Batch 执行结果 ===\n开始运行 EOD Batch...\n');
                console.log('Running ORE in EOD Batch directory:', workingDir);
                const eodResponse = await fetch(`${API_BASE_URL}/api/ore/run`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        workingDir,
                        date: selectedDate.format('YYYY-MM-DD'),
                        isLive,
                        marketFile: selectedMarketFile,
                        marketDataPath: MARKET_DATA_PATH
                    })
                });

                if (!eodResponse.ok) {
                    throw new Error(`EOD Batch execution failed: ${eodResponse.status}`);
                }

                // 处理EOD Batch结果
                const eodReader = eodResponse.body?.getReader();
                if (!eodReader) {
                    throw new Error('EOD response body is not readable');
                }

                let eodBuffer = '';
                const decoder = new TextDecoder();

                while (true) {
                    const { done, value } = await eodReader.read();
                    if (done) break;
                    
                    // 将新接收的数据添加到缓冲区
                    eodBuffer += decoder.decode(value, { stream: true });
                    
                    // 查找完整的行
                    const lines = eodBuffer.split('\n');
                    // 保留最后一个可能不完整的行
                    eodBuffer = lines.pop() || '';
                    
                    // 处理完整的行
                    if (lines.length > 0) {
                        setResult(prev => prev + lines.map(formatLine).join('\n') + '\n');
                    }
                }

                // 处理剩余的缓冲区
                if (eodBuffer) {
                    setResult(prev => prev + formatLine(eodBuffer) + '\n');
                }

                // 保存EOD Batch输出文件
                setResult(prev => prev + '\n正在保存 EOD Batch 输出文件...\n');
                const eodDate = selectedDate.format('YYYYMMDD');
                const eodTargetDir = `E:\\CursorAI\\OreResults\\EodBatch\\${eodDate}\\EOD`;
                console.log('Saving EOD Batch output files to:', eodTargetDir);
                const saveEodResponse = await fetch(`${API_BASE_URL}/api/ore/save-output`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        sourceDir: workingDir,
                        targetDir: eodTargetDir
                    })
                });

                if (!saveEodResponse.ok) {
                    throw new Error(`Failed to save EOD Batch output files: ${saveEodResponse.status}`);
                }
                setResult(prev => prev + 'EOD Batch 输出文件保存完成\n\n');

                // 3. 运行SOD Batch
                setResult(prev => prev + '=== SOD Batch 执行结果 ===\n开始运行 SOD Batch...\n');
                console.log('Running ORE in SOD Batch directory:', sodWorkingDir);
                const sodResponse = await fetch(`${API_BASE_URL}/api/ore/run`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        workingDir: sodWorkingDir,
                        date: selectedDate.format('YYYY-MM-DD'),
                        isLive,
                        marketFile: selectedMarketFile,
                        marketDataPath: MARKET_DATA_PATH
                    })
                });

                if (!sodResponse.ok) {
                    throw new Error(`SOD Batch execution failed: ${sodResponse.status}`);
                }

                // 处理SOD Batch结果
                const sodReader = sodResponse.body?.getReader();
                if (!sodReader) {
                    throw new Error('SOD response body is not readable');
                }

                let sodBuffer = '';

                while (true) {
                    const { done, value } = await sodReader.read();
                    if (done) break;
                    
                    // 将新接收的数据添加到缓冲区
                    sodBuffer += decoder.decode(value, { stream: true });
                    
                    // 查找完整的行
                    const lines = sodBuffer.split('\n');
                    // 保留最后一个可能不完整的行
                    sodBuffer = lines.pop() || '';
                    
                    // 处理完整的行
                    if (lines.length > 0) {
                        setResult(prev => prev + lines.map(formatLine).join('\n') + '\n');
                    }
                }

                // 处理剩余的缓冲区
                if (sodBuffer) {
                    setResult(prev => prev + formatLine(sodBuffer) + '\n');
                }

                // 保存SOD Batch输出文件
                setResult(prev => prev + '\n正在保存 SOD Batch 输出文件...\n');
                const sodDate = getNextBusinessDay(selectedDate).format('YYYYMMDD');  // 使用 SOD Batch 的日期
                const sodTargetDir = `E:\\CursorAI\\OreResults\\EodBatch\\${sodDate}\\SOD`;
                console.log('Saving SOD Batch output files to:', sodTargetDir);
                const saveSodResponse = await fetch(`${API_BASE_URL}/api/ore/save-output`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        sourceDir: sodWorkingDir,
                        targetDir: sodTargetDir
                    })
                });

                if (!saveSodResponse.ok) {
                    throw new Error(`Failed to save SOD Batch output files: ${saveSodResponse.status}`);
                }
                setResult(prev => prev + 'SOD Batch 输出文件保存完成\n\n');

                // 添加输出文件保存位置信息
                setResult(prev => prev + '=== 输出文件保存位置 ===\n' +
                    `EOD Batch: ${eodTargetDir}\\Output\n` +
                    `SOD Batch: ${sodTargetDir}\\Output`
                );

                message.success('Ore execution completed and output files saved for both EOD and SOD batches');
            }
        } catch (error) {
            console.error('Error running ORE:', error);
            message.error('Failed to run Ore: ' + (error instanceof Error ? error.message : String(error)));
        } finally {
            setLoading(false);
        }
    };

    const handlePrepareFiles = async () => {
        if (!selectedMarketFile) {
            setErrorMessage('Please select a market file first');
            return;
        }

        // 在EOD模式下检查文件是否存在
        if (!isLive) {
            const currentDate = selectedDate.format('YYYYMMDD');
            const expectedFileName = `IRFX Market_${currentDate}_Close.xls`;
            if (selectedMarketFile !== expectedFileName) {
                setErrorMessage(`市场数据文件 ${expectedFileName} 不存在`);
                return;
            }
        }

        setPreparingFiles(true);
        setErrorMessage('');
        setResult(null);  // 清除之前的结果

        try {
            // 获取当前工作目录
            const values = await form.validateFields();
            const workingDir = values.workingDir;
            const sodWorkingDir = values.sodWorkingDir;

            if (isLive) {
                // Live模式：只处理Live Batch
                const inputDir = `${workingDir}\\Input`;
                const result = await prepareMarketDataFile(inputDir, selectedDate);
                if (result.status === 'success') {
                    // 更新文件信息显示
                    const currentTime = dayjs().format('HH:mm:ss YYYY-MM-DD');
                    const marketDataFile = `ORE_Market_${selectedDate.format('YYYYMMDD')}.txt`;
                    const oreXmlFile = 'ore.xml';
                    
                    setUpdateInfo(
                        `Live Batch (${selectedDate.format('YYYY-MM-DD')}):\n` +
                        `${marketDataFile}, 更新于 ${currentTime}\n` +
                        `${oreXmlFile}, 更新于 ${currentTime}`
                    );
                } else {
                    throw new Error('Failed to prepare Live batch files');
                }
            } else {
                // EOD模式：分别处理EOD Batch和SOD Batch
                const eodInputDir = `${workingDir}\\Input`;
                const sodInputDir = `${sodWorkingDir}\\Input`;

                // 获取SOD的定价日期（下一个工作日）
                const sodDate = getNextBusinessDay(selectedDate);
                console.log('SOD pricing date:', sodDate.format('YYYY-MM-DD'));

                // 准备EOD Batch的市场数据文件
                const eodResult = await prepareMarketDataFile(eodInputDir, selectedDate);
                if (eodResult.status === 'success') {
                    // 准备SOD Batch的市场数据文件
                    const sodResult = await prepareMarketDataFile(sodInputDir, sodDate);
                    if (sodResult.status === 'success') {
                        // 更新文件信息显示
                        const currentTime = dayjs().format('HH:mm:ss YYYY-MM-DD');
                        const eodMarketDataFile = `ORE_Market_${selectedDate.format('YYYYMMDD')}.txt`;
                        const sodMarketDataFile = `ORE_Market_${sodDate.format('YYYYMMDD')}.txt`;
                        const oreXmlFile = 'ore.xml';
                        
                        // 更新文件信息显示，确保包含EOD和SOD的信息
                        const updateInfoText = 
                            `EOD Batch (${selectedDate.format('YYYY-MM-DD')}):\n` +
                            `${eodMarketDataFile}, 更新于 ${currentTime}\n` +
                            `${oreXmlFile}, 更新于 ${currentTime}\n\n` +
                            `SOD Batch (${sodDate.format('YYYY-MM-DD')}):\n` +
                            `${sodMarketDataFile}, 更新于 ${currentTime}\n` +
                            `${oreXmlFile}, 更新于 ${currentTime}`;
                        
                        console.log('Setting update info:', updateInfoText);
                        setUpdateInfo(updateInfoText);
                    } else {
                        throw new Error('Failed to prepare SOD batch files');
                    }
                } else {
                    throw new Error('Failed to prepare EOD batch files');
                }
            }
        } catch (err) {
            console.error('Error preparing files:', err);
            const errorMessage = err instanceof Error ? err.message : 'Failed to prepare ORE files';
            setErrorMessage(errorMessage);
        } finally {
            setPreparingFiles(false);
        }
    };

    // 准备市场数据文件的辅助函数
    const prepareMarketDataFile = async (inputDir: string, date: Dayjs): Promise<ApiResponse> => {
        const response = await fetch(`${API_BASE_URL}/api/market-data/convert-to-ore`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                basePath: MARKET_DATA_PATH,
                tm1File: selectedMarketFile,
                date: date.format('YYYY-MM-DD'),
                outputDir: inputDir
            }),
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const text = await response.text();
        try {
            return JSON.parse(text);
        } catch {
            console.error('Failed to parse response:', text);
            throw new Error('Invalid response format from server');
        }
    };

    return (
        <div style={{ padding: '20px' }}>
            <Card>
                <div style={{ marginBottom: '20px' }}>
                    <h1 style={{ margin: 0, marginBottom: '16px' }}>Ore Batch Controller</h1>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '16px', marginBottom: '16px' }}>
                        <Space>
                            <span>Batch Mode:</span>
                            <Switch 
                                checked={isLive} 
                                onChange={handleModeChange}
                                checkedChildren="Live"
                                unCheckedChildren="EOD"
                                className={isLive ? 'live-switch' : 'eod-switch'}
                            />
                        </Space>
                        <DatePicker
                            value={selectedDate}
                            onChange={handleDateChange}
                            format="YYYY-MM-DD"
                            style={{ width: '150px' }}
                            disabled={isLive}
                        />
                        <Select
                            value={selectedMarketFile}
                            onChange={handleMarketFileChange}
                            onDropdownVisibleChange={handleDropdownVisibleChange}
                            style={{ width: '250px' }}
                            options={marketFiles.map(file => ({
                                value: file,
                                label: file,
                                disabled: file.includes('不存在')
                            }))}
                            showSearch
                            filterOption={false}
                            notFoundContent={isLive ? "No files found" : "市场数据文件不存在"}
                            dropdownStyle={{ minWidth: '250px' }}
                            placeholder={isLive ? "Select market file" : "选择市场数据文件"}
                        />
                        {lastLoadTime && (
                            <span style={{ color: '#666' }}>
                                Last Update: {lastLoadTime}
                            </span>
                        )}
                    </div>
                </div>
                <Form 
                    form={form} 
                    layout="vertical" 
                    style={{ 
                        maxWidth: '600px', 
                        marginBottom: '20px',
                        ...formItemStyle
                    }}
                    initialValues={{
                        workingDir: isLive ? DEFAULT_LIVE_WORKING_DIR : DEFAULT_EOD_WORKING_DIR,
                        sodWorkingDir: isLive ? undefined : DEFAULT_SOD_WORKING_DIR
                    }}
                >
                    <Form.Item
                        name="workingDir"
                        label={isLive ? "Live Batch Working Directory" : "EOD Batch Working Directory"}
                        rules={[{ required: true, message: 'Please input working directory!' }]}
                        style={{ 
                            marginBottom: '24px'
                        }}
                    >
                        <Input 
                            placeholder={`Enter ${isLive ? 'Live' : 'EOD'} batch working directory path`} 
                            style={{ width: '100%' }} 
                            value={isLive ? DEFAULT_LIVE_WORKING_DIR : DEFAULT_EOD_WORKING_DIR}
                            onChange={(e) => form.setFieldsValue({ workingDir: e.target.value })}
                        />
                    </Form.Item>

                    {!isLive && (
                        <Form.Item
                            name="sodWorkingDir"
                            label="T+1 SOD Batch Working Directory"
                            rules={[{ required: true, message: 'Please input SOD working directory!' }]}
                            style={{ 
                                marginBottom: '24px',
                                marginTop: '0px'
                            }}
                        >
                            <Input 
                                placeholder="Enter T+1 SOD batch working directory path" 
                                style={{ width: '100%' }} 
                                value={DEFAULT_SOD_WORKING_DIR}
                                onChange={(e) => form.setFieldsValue({ sodWorkingDir: e.target.value })}
                            />
                        </Form.Item>
                    )}

                    <div style={{ 
                        display: 'flex', 
                        flexDirection: 'column',  // 改为纵向排列
                        gap: '16px',
                        marginBottom: '24px',
                        marginTop: '32px'
                    }}>
                        <Button 
                            type="primary" 
                            onClick={handlePrepareFiles}
                            loading={preparingFiles}
                            style={{ width: 'fit-content' }}  // 按钮宽度适应内容
                        >
                            Prepare ORE Files
                        </Button>
                        
                        {updateInfo && (
                            <textarea
                                readOnly
                                value={updateInfo}
                                style={{
                                    width: '100%',  // 宽度与 Working Directory 输入框一致
                                    padding: '8px 12px',
                                    border: '1px solid #d9d9d9',
                                    borderRadius: '4px',
                                    backgroundColor: '#fafafa',
                                    fontSize: '14px',
                                    resize: 'none',
                                    height: '60px'
                                }}
                            />
                        )}
                        {errorMessage && (
                            <div style={{ 
                                color: '#ff4d4f', 
                                fontSize: '14px',
                                position: 'absolute',
                                top: '100%',
                                left: '0'
                            }}>
                                {errorMessage}
                            </div>
                        )}
                    </div>

                    <Form.Item style={{ marginTop: '16px' }}>
                        <Button onClick={handleTest} style={{ marginRight: '10px' }}>
                            Test Connection
                        </Button>
                        <Button type="primary" onClick={handleRunOre} loading={loading}>
                            Run Ore
                        </Button>
                    </Form.Item>
                </Form>
                {result && (
                    <div style={{ marginTop: '20px', whiteSpace: 'pre-wrap' }}>
                        <h2>Result:</h2>
                        <p>{result}</p>
                    </div>
                )}
            </Card>
            <style>
                {`
                    .live-switch.ant-switch-checked {
                        background-color: #ff4d4f !important;
                    }
                    .eod-switch {
                        background-color: #1890ff !important;
                    }
                    .ant-form-item {
                        margin-bottom: 0 !important;
                    }
                    .ant-form-item-label {
                        padding-bottom: 0 !important;
                        line-height: 1 !important;
                    }
                    .ant-form-item-control {
                        margin-top: 0 !important;
                        line-height: 1 !important;
                    }
                `}
            </style>
        </div>
    );
};

export default Ore; 
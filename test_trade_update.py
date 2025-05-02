import csv
import time
from datetime import datetime, timedelta
import os

def generate_trade(trade_id, timestamp):
    # 模拟不同的交易数据，基于实际的外汇交易数据格式
    trades = [
        # Book BBG ID, Calypso ID, Strategy, Undl, Notional, Cpty, Product Type, Asset Class, Multi, strike, Cutoff
        ('CIFXDH', f'140970{trade_id}', f'295012{trade_id}', 'USDCNH', 25000000, 'HK-GSIG', 'FX Spot', 'FX', 1, 7.31532, 'FXOPT'),
        ('CIFXDH', f'140970{trade_id}', f'295012{trade_id}', 'EURUSD', 15000000, 'HK-GSIG', 'FX Spot', 'FX', 1, 1.07850, 'FXOPT'),
        ('CIFXDH', f'140970{trade_id}', f'295012{trade_id}', 'USDJPY', 20000000, 'HK-GSIG', 'FX Spot', 'FX', 1, 149.250, 'FXOPT'),
        ('CIFXDH', f'140970{trade_id}', f'295012{trade_id}', 'GBPUSD', 10000000, 'HK-GSIG', 'FX Spot', 'FX', 1, 1.26350, 'FXOPT'),
    ]
    
    # 循环使用测试数据
    index = (trade_id - 1) % len(trades)
    trade = trades[index]
    
    # 计算结算日期（交易日期 + 7天）
    settle_date = (timestamp + timedelta(days=7)).strftime('%Y%m%d')
    
    return [
        timestamp.strftime('%Y%m%d %H:%M:%S'),    # Trade Date (添加时分秒)
        timestamp.strftime('%Y%m%d'),             # Valuation Date
        trade[0],                                 # Book
        trade[1],                                 # BBG ID
        trade[2],                                 # Calypso ID
        "myStra_" + timestamp.strftime('%Y%m%d'), # Strategy
        trade[3],                                 # Undl
        str(trade[4]),                           # Notional
        trade[5],                                # Cpty
        trade[6],                                # Product Type
        trade[7],                                # Asset Class
        str(trade[8]),                           # OptMult
        '########',                              # Expiry Date
        settle_date,                             # Settl. Date
        'Strike',                                # Strike
        str(trade[9])                            # Cutoff
    ]

def update_trades():
    # 修改文件路径，与Java服务配置一致
    file_path = r'E:\CursorAI\PMS\data\trades.csv'
    headers = [
        'Trade Date', 'Valuation Date', 'Book', 'BBG ID', 'Calypso ID', 'Strategy',
        'Undl', 'Notional', 'Cpty', 'Product Type', 'Asset Class', 'OptMult',
        'Expiry Date', 'Settl. Date', 'Strike', 'Cutoff'
    ]
    
    try:
        # 确保目录存在
        os.makedirs(os.path.dirname(file_path), exist_ok=True)
        
        # 创建或清空文件并写入表头
        with open(file_path, 'w', newline='') as f:
            writer = csv.writer(f)
            writer.writerow(headers)
            print("Created trade file with headers")
        
        current_id = 1
        while True:
            timestamp = datetime.now()
            new_trade = generate_trade(current_id, timestamp)
            
            try:
                # 读取现有数据
                with open(file_path, 'r') as f:
                    trades = list(csv.reader(f))
                    if len(trades) > 20:  # 保持最新的10条记录
                        trades = trades[:1] + trades[-9:]  # 保留表头和最新的9条记录
                
                # 添加新交易
                trades.append(new_trade)
                
                # 写回文件
                with open(file_path, 'w', newline='') as f:
                    writer = csv.writer(f)
                    writer.writerows(trades)
                
                print(f"Added trade at {timestamp.strftime('%Y-%m-%d %H:%M:%S')}:")
                print(f"  Calypso ID: {new_trade[4]}")
                print(f"  Book: {new_trade[2]}")
                print(f"  Instrument: {new_trade[6]}")
                print(f"  Notional: {new_trade[7]}")
                print(f"  Cutoff: {new_trade[15]}")
                print("-" * 50)
                
            except Exception as e:
                print(f"Error updating trades: {str(e)}")
            
            current_id += 1
            time.sleep(10)  # 每10秒更新一次（从30秒改为10秒）
            
    except KeyboardInterrupt:
        print("\nStopped trade updates.")
    except Exception as e:
        print(f"Fatal error: {str(e)}")

if __name__ == '__main__':
    update_trades() 
import pandas as pd
import time
import os
from datetime import datetime
from typing import Optional

class TradeMonitor:
    def __init__(self, file_path: str):
        self.file_path = file_path
        self.last_modified_time: Optional[float] = None
        self.trades_data = pd.DataFrame()

    def check_file_update(self) -> bool:
        """检查文件是否被更新"""
        if not os.path.exists(self.file_path):
            return False
        
        current_mtime = os.path.getmtime(self.file_path)
        if self.last_modified_time is None or current_mtime > self.last_modified_time:
            self.last_modified_time = current_mtime
            return True
        return False

    def read_trades(self) -> dict:
        """读取交易数据"""
        try:
            self.trades_data = pd.read_csv(self.file_path)
            # 转换为字典列表格式，方便JSON序列化
            trades_list = self.trades_data.to_dict('records')
            return {
                "status": "success",
                "timestamp": datetime.now().isoformat(),
                "data": trades_list
            }
        except Exception as e:
            return {
                "status": "error",
                "timestamp": datetime.now().isoformat(),
                "error": str(e)
            } 
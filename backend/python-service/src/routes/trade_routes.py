from fastapi import APIRouter, HTTPException
from ..services.trade_monitor import TradeMonitor
import asyncio
from typing import Dict, Any

router = APIRouter()
trade_monitor = TradeMonitor(r"E:\CursorAI\PMS\Data\trade_live.csv")

@router.get("/trades")
async def get_trades() -> Dict[str, Any]:
    """获取最新的交易数据"""
    if trade_monitor.check_file_update():
        return trade_monitor.read_trades()
    return {
        "status": "no_update",
        "timestamp": None,
        "data": []
    }

# 后台任务，定期检查文件更新
async def monitor_trades():
    while True:
        await asyncio.sleep(60)  # 每60秒检查一次
        if trade_monitor.check_file_update():
            trade_monitor.read_trades() 
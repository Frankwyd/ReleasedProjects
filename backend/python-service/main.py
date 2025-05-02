from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from src.routes import trade_routes
import asyncio

app = FastAPI(title="Trading PnL Calculation Engine")

# 配置CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 在生产环境中应该设置具体的源
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 注册路由
app.include_router(trade_routes.router, prefix="/api")

@app.on_event("startup")
async def startup_event():
    # 启动后台监控任务
    asyncio.create_task(trade_routes.monitor_trades())

@app.get("/")
async def root():
    return {"message": "Trading PnL Calculation Engine"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000) 
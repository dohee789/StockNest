import React, { useState } from 'react';
import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { AppProvider } from './context/AppContext';
import Header from './layout/Header/Header';
import MarketIndex from './layout/MarketIndex/MarketIndex';
import BottomNav from './layout/BottomNav/BottomNav';
import Sidebar from './layout/Sidebar/Sidebar';
import HomePage from './pages/Home/HomePage';
import StocksPage from './pages/Stocks/StocksPage';
import OrderPage from './pages/Order/OrderPage';
import PortfolioPage from './pages/Portfolio/PortfolioPage';
import TradeHistoryPage from './pages/History/TradeHistoryPage';
import MyPage from './pages/My/MyPage';
import StockDetailPage from './pages/StockDetail/StockDetailPage';
import KakaoLogin from './features/KakaoLogin/KakaoLogin';
import PendingOrdersPage from './pages/PendingOrders/PendingOrdersPage';
import FuturesPage from './pages/Futures/FuturesPage';
import FuturesDetailPage from './pages/Futures/FuturesDetailPage';
import FuturesPositionsPage from './pages/Futures/FuturesPositionsPage';
import EtfPage from './pages/Etf/EtfPage';
import './App.css';

const App = () => {
    const [menuOpen, setMenuOpen] = useState(false);

    return (
        <AppProvider>
            <BrowserRouter>
                <div className="appContainer">
                    <Header onMenuOpen={() => setMenuOpen(true)} />
                    <MarketIndex />
                    <div className="contentWrapper">
                        <Sidebar mobileOpen={menuOpen} onClose={() => setMenuOpen(false)} />
                        <main className="pageContent">
                            <Routes>
                                <Route path="/" element={<HomePage />} />
                                <Route path="/stocks" element={<StocksPage />} />
                                <Route path="/stock/:code" element={<StockDetailPage />} />
                                <Route path="/order" element={<OrderPage />} />
                                <Route path="/portfolio" element={<PortfolioPage />} />
                                <Route path="/history" element={<TradeHistoryPage />} />
                                <Route path="/my" element={<MyPage />} />
                                <Route path="/orders/pending" element={<PendingOrdersPage />} />
                                <Route path="/etf" element={<EtfPage />} />
                                <Route path="/futures" element={<FuturesPage />} />
                                <Route path="/futures/positions" element={<FuturesPositionsPage />} />
                                <Route path="/futures/:code" element={<FuturesDetailPage />} />
                                <Route path="/KakaoLogin/:code" element={<KakaoLogin />} />
                            </Routes>
                        </main>
                    </div>
                    <BottomNav />
                </div>
                <Toaster
                    position="top-center"
                    toastOptions={{
                        style: {
                            background: '#FFFDF7',
                            color: '#1C1300',
                            border: '1px solid #EDE5D0',
                            borderRadius: '14px',
                            fontSize: '14px',
                            fontWeight: 500,
                            boxShadow: '0 4px 16px rgba(234, 179, 8, 0.1)',
                        },
                        success: {
                            iconTheme: { primary: '#EAB308', secondary: '#FFFFFF' },
                        },
                        error: {
                            iconTheme: { primary: '#EF4444', secondary: '#FFFFFF' },
                        },
                    }}
                />
            </BrowserRouter>
        </AppProvider>
    );
};

export default App;

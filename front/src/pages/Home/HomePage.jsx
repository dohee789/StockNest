import React, { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useApp } from '../../context/AppContext';
import { useWebSocket } from '../../hooks/useWebSocket';
import { IconChartBar, IconChartCandle, IconHistory, IconTrendingUp, IconTrendingDown, IconSchool } from '@tabler/icons-react';
import WatchlistCard from './WatchlistCard';
import PortfolioSummaryCard from './PortfolioSummaryCard';
import LoginPrompt from './LoginPrompt';
import StockSearchBar from './StockSearchBar';
import AiMarketBriefingCard from './AiMarketBriefingCard';
import AiRecommendationsCard from './AiRecommendationsCard';
import { API_BASE_URL } from '../../config/api';
import classes from './HomePage.module.css';

const BASE_URL = API_BASE_URL;

const HomePage = () => {
    const navigate = useNavigate();
    const { userId, isLoggedIn, realtimePrices } = useApp();
    const [watchlist, setWatchlist] = useState([]);
    const [loading, setLoading] = useState(false);
    const [rising, setRising] = useState([]);
    const [falling, setFalling] = useState([]);
    const [rankTab, setRankTab] = useState('rising'); // rising | falling
    const [risingLoaded, setRisingLoaded] = useState(false);
    const [fallingLoaded, setFallingLoaded] = useState(false);

    useWebSocket();

    const fetchWatchlist = useCallback(async () => {
        if (!userId) return;
        setLoading(true);
        try {
            const res = await fetch(`${BASE_URL}/stock-data`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ id: userId }),
            });
            if (res.ok) {
                const data = await res.json();
                setWatchlist(data || []);
            }
        } catch (e) {
            console.error(e);
        } finally {
            setLoading(false);
        }
    }, [userId]);

    const fetchRising = useCallback(async () => {
        if (risingLoaded) return;
        try {
            const res = await fetch(`${BASE_URL}/sise/rising`);
            if (res.ok) {
                setRising((await res.json() || []).slice(0, 20));
            }
        } catch { /* ignore */ }
        setRisingLoaded(true);
    }, [risingLoaded]);

    const fetchFalling = useCallback(async () => {
        if (fallingLoaded) return;
        try {
            const res = await fetch(`${BASE_URL}/sise/falling`);
            if (res.ok) {
                setFalling((await res.json() || []).slice(0, 20));
            }
        } catch { /* ignore */ }
        setFallingLoaded(true);
    }, [fallingLoaded]);

    useEffect(() => {
        fetchWatchlist();
        fetchRising();
    }, [fetchWatchlist, fetchRising]);

    useEffect(() => {
        if (rankTab === 'falling') fetchFalling();
    }, [rankTab, fetchFalling]);

    const mergedWatchlist = watchlist.map(stock => {
        const rt = realtimePrices[stock.code];
        return rt ? { ...stock, price: rt.price, change: rt.change, changeRate: rt.changeRate } : stock;
    });

    const isUp = (rate) => rate && (rate.includes('▲') || rate.startsWith('+'));
    const isDown = (rate) => rate && (rate.includes('▼') || rate.startsWith('-'));

    return (
        <div className={classes.page}>
            <StockSearchBar onStockAdded={fetchWatchlist} />

            {!isLoggedIn && (
                <button className={classes.tutorialBanner} onClick={() => navigate('/tutorial')}>
                    <IconSchool size={18} stroke={1.5} />
                    <span>주식이 처음이라면? <strong>투자 가이드</strong>로 시작해보세요</span>
                </button>
            )}

            {isLoggedIn && <AiMarketBriefingCard />}

            {/* 급등 / 급락 종목 수평 스크롤 */}
            <section className={classes.risingSection}>
                <div className={classes.risingSectionHeader}>
                    <div className={classes.rankTabs}>
                        <button
                            className={`${classes.rankTab} ${rankTab === 'rising' ? classes.rankTabRise : ''}`}
                            onClick={() => setRankTab('rising')}
                        >
                            <IconTrendingUp size={14} stroke={1.5} /> 급등
                        </button>
                        <button
                            className={`${classes.rankTab} ${rankTab === 'falling' ? classes.rankTabFall : ''}`}
                            onClick={() => setRankTab('falling')}
                        >
                            <IconTrendingDown size={14} stroke={1.5} /> 급락
                        </button>
                    </div>
                </div>
                <div className={classes.risingScroll}>
                    {(rankTab === 'rising' ? rising : falling).map((s, i) => (
                        <div
                            key={s.code || i}
                            className={classes.risingCard}
                            onClick={() => navigate(`/stock/${s.code}`)}
                        >
                            <div className={classes.risingRank}>{i + 1}</div>
                            <div className={classes.risingName}>{s.name}</div>
                            <div className={classes.risingPrice}>{s.price}원</div>
                            <div className={`${classes.risingRate} ${isUp(s.changeRate) ? classes.rise : isDown(s.changeRate) ? classes.fall : ''}`}>
                                {s.changeRate}
                            </div>
                        </div>
                    ))}
                    {(rankTab === 'rising' ? rising : falling).length === 0 && (
                        <div className={classes.rankLoading}>
                            {(rankTab === 'rising' ? risingLoaded : fallingLoaded)
                                ? '장 마감 후에는 데이터가 없을 수 있습니다'
                                : '불러오는 중...'}
                        </div>
                    )}
                </div>
            </section>

            {/* 응용반 바로가기 (모바일에서 하단 네비에 없는 메뉴) */}
            <section className={classes.quickLinks}>
                <button className={classes.quickLink} onClick={() => navigate('/etf')}
                    data-tooltip="여러 주식을 한 바구니에 담은 상품">
                    <IconChartBar size={18} stroke={1.5} />
                    <span>ETF</span>
                </button>
                <button className={classes.quickLink} onClick={() => navigate('/futures')}
                    data-tooltip="레버리지 상품 — 고위험 고수익">
                    <IconChartCandle size={18} stroke={1.5} />
                    <span>선물</span>
                </button>
                <button className={classes.quickLink} onClick={() => navigate('/history')}>
                    <IconHistory size={18} stroke={1.5} />
                    <span>거래내역</span>
                </button>
            </section>

            {isLoggedIn ? (
                <>
                    <PortfolioSummaryCard />
                    <AiRecommendationsCard />
                    <section className={classes.section}>
                        <div className={classes.sectionHeader}>
                            <h2 className={classes.sectionTitle}>관심종목</h2>
                            <span className={classes.sectionCount}>{mergedWatchlist.length}</span>
                        </div>
                        {loading ? (
                            <div className={classes.skeleton}>
                                {[1, 2, 3].map(i => <div key={i} className={classes.skeletonCard} />)}
                            </div>
                        ) : mergedWatchlist.length === 0 ? (
                            <div className={classes.empty}>
                                <p>관심종목을 추가해보세요</p>
                                <span>위 검색창에서 종목을 검색하여 추가할 수 있습니다</span>
                            </div>
                        ) : (
                            <div className={classes.watchlist}>
                                {mergedWatchlist.map(stock => (
                                    <WatchlistCard
                                        key={stock.code || stock.id}
                                        stock={stock}
                                        onRemove={fetchWatchlist}
                                    />
                                ))}
                            </div>
                        )}
                    </section>
                </>
            ) : (
                <LoginPrompt />
            )}
        </div>
    );
};

export default HomePage;

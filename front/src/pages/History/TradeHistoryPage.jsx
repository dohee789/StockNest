import React, { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useApp } from '../../context/AppContext';
import CallMadeRoundedIcon from '@mui/icons-material/CallMadeRounded';
import CallReceivedRoundedIcon from '@mui/icons-material/CallReceivedRounded';
import { API_BASE_URL } from '../../config/api';
import classes from './TradeHistoryPage.module.css';

const BASE_URL = `${API_BASE_URL}/trade-history`;

const PERIODS = [
    { label: '1주', days: 7 },
    { label: '1달', days: 30 },
    { label: '3달', days: 90 },
    { label: '전체', days: 0 },
];

const TradeHistoryPage = () => {
    const navigate = useNavigate();
    const { userId, isLoggedIn } = useApp();
    const [histories, setHistories] = useState([]);
    const [loading, setLoading] = useState(false);
    const [filter, setFilter] = useState('ALL'); // ALL | BUY | SELL
    const [period, setPeriod] = useState(0); // 0 = 전체

    const fetchHistories = useCallback(async () => {
        if (!userId) return;
        setLoading(true);
        try {
            const res = await fetch(BASE_URL, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ customerId: userId }),
            });
            if (res.ok) {
                setHistories(await res.json() || []);
            }
        } catch (e) {
            console.error(e);
        } finally {
            setLoading(false);
        }
    }, [userId]);

    useEffect(() => {
        fetchHistories();
    }, [fetchHistories]);

    const now = Date.now();
    const periodFiltered = period === 0
        ? histories
        : histories.filter(h => {
            const t = new Date(h.tradeTime).getTime();
            return now - t <= period * 24 * 60 * 60 * 1000;
        });

    const filtered = filter === 'ALL' ? periodFiltered : periodFiltered.filter(h => h.tradeType === filter);

    // 실현손익 계산 (매도 기준)
    const totalSellAmount = filtered
        .filter(h => h.tradeType === 'SELL')
        .reduce((s, h) => s + (h.totalPrice || 0), 0);
    const totalBuyAmount = filtered
        .filter(h => h.tradeType === 'BUY')
        .reduce((s, h) => s + (h.totalPrice || 0), 0);
    const realizedPnl = totalSellAmount - totalBuyAmount;

    const fmt = (n) => (n || 0).toLocaleString('ko-KR');

    const formatDate = (dateStr) => {
        if (!dateStr) return '';
        const d = new Date(dateStr);
        return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
    };

    if (!isLoggedIn) return <div className={classes.notLogin}>로그인이 필요합니다</div>;

    return (
        <div className={classes.page}>
            <div className={classes.header}>
                <h1 className={classes.title}>거래내역</h1>
            </div>

            {/* 기간 필터 */}
            <div className={classes.periodRow}>
                {PERIODS.map(p => (
                    <button
                        key={p.days}
                        className={`${classes.periodBtn} ${period === p.days ? classes.activePeriod : ''}`}
                        onClick={() => setPeriod(p.days)}
                    >
                        {p.label}
                    </button>
                ))}
            </div>

            {/* 거래 요약 */}
            <div className={classes.summaryCard}>
                <div className={classes.summaryRow}>
                    <span className={classes.summaryLabel}>총 매수</span>
                    <span className={`${classes.summaryValue} ${classes.buyColor}`}>-{fmt(totalBuyAmount)}원</span>
                </div>
                <div className={classes.summaryRow}>
                    <span className={classes.summaryLabel}>총 매도</span>
                    <span className={`${classes.summaryValue} ${classes.sellColor}`}>+{fmt(totalSellAmount)}원</span>
                </div>
                <div className={classes.divider} />
                <div className={classes.summaryRow}>
                    <span className={classes.summaryLabel}>실현손익</span>
                    <span className={`${classes.summaryValue} ${classes.summaryMain} ${realizedPnl >= 0 ? classes.sellColor : classes.buyColor}`}>
                        {realizedPnl >= 0 ? '+' : ''}{fmt(realizedPnl)}원
                    </span>
                </div>
                <div className={classes.summaryCount}>{filtered.length}건</div>
            </div>

            {/* 유형 필터 */}
            <div className={classes.filters}>
                {['ALL', 'BUY', 'SELL'].map(f => (
                    <button
                        key={f}
                        className={`${classes.filterBtn} ${filter === f ? classes.activeFilter : ''}`}
                        onClick={() => setFilter(f)}
                    >
                        {f === 'ALL' ? '전체' : f === 'BUY' ? '매수' : '매도'}
                    </button>
                ))}
            </div>

            {loading ? (
                <div className={classes.loading}>로딩 중...</div>
            ) : filtered.length === 0 ? (
                <div className={classes.empty}>거래내역이 없습니다</div>
            ) : (
                <div className={classes.list}>
                    {filtered.map(h => (
                        <div
                            key={h.id}
                            className={classes.historyCard}
                            onClick={() => navigate(`/stock/${h.stockCode}`)}
                            style={{ cursor: 'pointer' }}
                        >
                            <div className={`${classes.typeIcon} ${h.tradeType === 'BUY' ? classes.buyIcon : classes.sellIcon}`}>
                                {h.tradeType === 'BUY'
                                    ? <CallReceivedRoundedIcon fontSize="small" />
                                    : <CallMadeRoundedIcon fontSize="small" />}
                            </div>
                            <div className={classes.info}>
                                <div className={classes.infoTop}>
                                    <div>
                                        <span className={classes.stockName}>{h.stockName}</span>
                                        <span className={classes.stockCode}>{h.stockCode}</span>
                                    </div>
                                    <span className={`${classes.totalPrice} ${h.tradeType === 'BUY' ? classes.buyColor : classes.sellColor}`}>
                                        {h.tradeType === 'BUY' ? '-' : '+'}{fmt(h.totalPrice)}원
                                    </span>
                                </div>
                                <div className={classes.infoBottom}>
                                    <span>{h.amount}주 · 주당 {fmt(h.pricePerStock)}원</span>
                                    <span className={classes.time}>{formatDate(h.tradeTime)}</span>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

export default TradeHistoryPage;

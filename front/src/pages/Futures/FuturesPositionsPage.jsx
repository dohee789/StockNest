import React, { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useApp } from '../../context/AppContext';
import toast from 'react-hot-toast';
import classes from './FuturesPositionsPage.module.css';

const BASE_URL = 'http://localhost:8090';

const STATUS_LABEL = { OPEN: '보유중', CLOSED: '청산됨', LIQUIDATED: '강제청산' };
const STATUS_COLOR = { OPEN: 'var(--accent)', CLOSED: 'var(--text-muted)', LIQUIDATED: 'var(--rise)' };

const FuturesPositionsPage = () => {
    const { userId, isLoggedIn } = useApp();
    const navigate = useNavigate();
    const [positions, setPositions] = useState([]);
    const [loading, setLoading] = useState(true);
    const [filter, setFilter] = useState('OPEN');
    const [closing, setClosing] = useState(null);

    const fetchPositions = useCallback(async () => {
        if (!userId) return;
        try {
            const res = await fetch(`${BASE_URL}/futures/positions/${userId}`);
            if (res.ok) setPositions(await res.json());
        } catch { /* ignore */ } finally {
            setLoading(false);
        }
    }, [userId]);

    useEffect(() => {
        if (!isLoggedIn) { navigate('/'); return; }
        fetchPositions();
        const id = setInterval(fetchPositions, 30000);
        return () => clearInterval(id);
    }, [isLoggedIn, fetchPositions, navigate]);

    const handleClose = async (pos) => {
        if (!window.confirm(`${pos.futuresName} 포지션을 청산하시겠습니까?`)) return;
        setClosing(pos.id);
        try {
            const res = await fetch(`${BASE_URL}/futures/close`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ positionId: pos.id, customerId: userId }),
            });
            if (res.ok) {
                toast.success('포지션이 청산되었습니다');
                fetchPositions();
            } else {
                const msg = await res.text();
                toast.error(msg || '청산에 실패했습니다');
            }
        } catch {
            toast.error('오류가 발생했습니다');
        } finally {
            setClosing(null);
        }
    };

    const filtered = positions.filter(p => filter === 'ALL' || p.status === filter);
    const openPositions = positions.filter(p => p.status === 'OPEN');
    const totalPnl = openPositions.reduce((sum, p) => sum + (p.unrealizedPnl ?? 0), 0);
    const totalMargin = openPositions.reduce((sum, p) => sum + p.marginAmount, 0);

    return (
        <div className={classes.page}>
            <div className={classes.pageHeader}>
                <h2 className={classes.title}>내 선물 포지션</h2>
                <button className={classes.toMarketBtn} onClick={() => navigate('/futures')}>선물 시장 →</button>
            </div>

            {/* 요약 카드 */}
            {openPositions.length > 0 && (
                <div className={classes.summaryCard}>
                    <div className={classes.summaryItem}>
                        <span className={classes.summaryLabel}>투입 증거금</span>
                        <span className={classes.summaryValue}>{totalMargin.toLocaleString('ko-KR')}원</span>
                    </div>
                    <div className={classes.summaryItem}>
                        <span className={classes.summaryLabel}>평가손익</span>
                        <span className={`${classes.summaryValue} ${totalPnl >= 0 ? classes.rise : classes.fall}`}>
                            {totalPnl >= 0 ? '+' : ''}{totalPnl.toLocaleString('ko-KR')}원
                        </span>
                    </div>
                    <div className={classes.summaryItem}>
                        <span className={classes.summaryLabel}>평가금액</span>
                        <span className={classes.summaryValue}>{(totalMargin + totalPnl).toLocaleString('ko-KR')}원</span>
                    </div>
                </div>
            )}

            {/* 필터 탭 */}
            <div className={classes.tabs}>
                {['OPEN', 'CLOSED', 'ALL'].map(tab => (
                    <button
                        key={tab}
                        className={`${classes.tab} ${filter === tab ? classes.activeTab : ''}`}
                        onClick={() => setFilter(tab)}
                    >
                        {tab === 'OPEN' ? '보유중' : tab === 'CLOSED' ? '청산됨' : '전체'}
                    </button>
                ))}
            </div>

            {loading ? (
                <div className={classes.empty}>로딩 중...</div>
            ) : filtered.length === 0 ? (
                <div className={classes.empty}>
                    <p>{filter === 'OPEN' ? '보유 중인 포지션이 없습니다' : '내역이 없습니다'}</p>
                    {filter === 'OPEN' && (
                        <button className={classes.goBtn} onClick={() => navigate('/futures')}>선물 거래 시작</button>
                    )}
                </div>
            ) : (
                <div className={classes.list}>
                    {filtered.map(pos => (
                        <PositionCard
                            key={pos.id}
                            pos={pos}
                            onClose={handleClose}
                            closing={closing === pos.id}
                        />
                    ))}
                </div>
            )}
        </div>
    );
};

const PositionCard = ({ pos, onClose, closing }) => {
    const isOpen = pos.status === 'OPEN';
    const pnl = isOpen ? pos.unrealizedPnl : pos.realizedPnl;
    const pnlRate = pos.unrealizedPnlRate;
    const isPos = pnl != null && pnl >= 0;

    const formatPrice = (p) => p != null
        ? p.toLocaleString('en-US', { maximumFractionDigits: 2 }) + ` ${pos.currency}`
        : '-';

    return (
        <div className={`${classes.card} ${!isOpen ? classes.closedCard : ''}`}>
            <div className={classes.cardTop}>
                <div className={classes.cardLeft}>
                    <span
                        className={classes.dirTag}
                        style={{ color: pos.direction === 'LONG' ? 'var(--rise)' : 'var(--fall)' }}
                    >
                        {pos.direction === 'LONG' ? '▲ 롱' : '▼ 숏'}
                    </span>
                    <span className={classes.cardName}>{pos.futuresName}</span>
                    <span className={classes.leverage}>{pos.leverage}x 레버리지</span>
                </div>
                <div>
                    <span
                        className={classes.statusBadge}
                        style={{ color: STATUS_COLOR[pos.status] }}
                    >
                        {STATUS_LABEL[pos.status]}
                    </span>
                </div>
            </div>

            <div className={classes.cardBody}>
                <div className={classes.priceRow}>
                    <span className={classes.priceLabel}>진입가</span>
                    <span className={classes.priceValue}>{formatPrice(pos.entryPrice)}</span>
                </div>
                {isOpen && (
                    <div className={classes.priceRow}>
                        <span className={classes.priceLabel}>현재가</span>
                        <span className={classes.priceValue}>
                            {pos.currentPrice != null ? formatPrice(pos.currentPrice) : '조회 중...'}
                        </span>
                    </div>
                )}
                <div className={classes.priceRow}>
                    <span className={classes.priceLabel}>증거금</span>
                    <span className={classes.priceValue}>{pos.marginAmount.toLocaleString('ko-KR')}원</span>
                </div>
                <div className={classes.priceRow}>
                    <span className={classes.priceLabel}>{isOpen ? '평가손익' : '실현손익'}</span>
                    <span className={`${classes.priceValue} ${isPos ? classes.rise : classes.fall}`}>
                        {pnl != null
                            ? `${isPos ? '+' : ''}${pnl.toLocaleString('ko-KR')}원 (${isPos ? '+' : ''}${pnlRate?.toFixed(1) ?? '?'}%)`
                            : '계산 중...'
                        }
                    </span>
                </div>
            </div>

            {isOpen && (
                <button
                    className={classes.closeBtn}
                    onClick={() => onClose(pos)}
                    disabled={closing}
                >
                    {closing ? '청산 중...' : '포지션 청산'}
                </button>
            )}
        </div>
    );
};

export default FuturesPositionsPage;

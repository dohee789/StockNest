import React, { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useApp } from '../../context/AppContext';
import toast from 'react-hot-toast';
import classes from './FuturesDetailPage.module.css';

const BASE_URL = 'http://localhost:8090';
const LEVERAGES = [2, 5, 10, 20];
const QUICK_PCT = [10, 25, 50, 100];

const FuturesDetailPage = () => {
    const { code } = useParams();
    const { userId, isLoggedIn } = useApp();
    const navigate = useNavigate();

    const [item, setItem] = useState(null);
    const [account, setAccount] = useState(null);
    const [direction, setDirection] = useState('LONG');
    const [leverage, setLeverage] = useState(10);
    const [marginInput, setMarginInput] = useState('');
    const [loading, setLoading] = useState(false);

    const fetchDetail = useCallback(async () => {
        try {
            const res = await fetch(`${BASE_URL}/futures/${code}`);
            if (res.ok) setItem(await res.json());
        } catch { /* ignore */ }
    }, [code]);

    const fetchAccount = useCallback(async () => {
        if (!userId) return;
        try {
            const res = await fetch(`${BASE_URL}/info/account`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ customerId: userId }),
            });
            if (res.ok) setAccount(await res.json());
        } catch { /* ignore */ }
    }, [userId]);

    useEffect(() => {
        fetchDetail();
        fetchAccount();
        const id = setInterval(fetchDetail, 30000);
        return () => clearInterval(id);
    }, [fetchDetail, fetchAccount]);

    const deposit = account?.deposit ?? 0;
    const marginNum = parseInt(marginInput.replace(/,/g, ''), 10) || 0;
    const positionSize = marginNum * leverage;

    const handleMarginChange = (e) => {
        const raw = e.target.value.replace(/[^0-9]/g, '');
        setMarginInput(raw ? parseInt(raw, 10).toLocaleString('ko-KR') : '');
    };

    const applyPct = (pct) => {
        const amount = Math.floor(deposit * pct / 100 / 10000) * 10000;
        setMarginInput(amount > 0 ? amount.toLocaleString('ko-KR') : '');
    };

    const handleOpen = async () => {
        if (!isLoggedIn) { toast.error('로그인이 필요합니다'); return; }
        if (!account) { toast.error('계좌를 먼저 개설해주세요'); return; }
        if (marginNum <= 0) { toast.error('증거금을 입력해주세요'); return; }
        if (marginNum > deposit) { toast.error('잔액이 부족합니다'); return; }

        setLoading(true);
        try {
            const res = await fetch(`${BASE_URL}/futures/open`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    customerId: userId,
                    futuresCode: code,
                    direction,
                    marginAmount: marginNum,
                    leverage,
                }),
            });
            if (res.ok) {
                toast.success(`${item?.name} ${direction === 'LONG' ? '롱' : '숏'} 진입 완료`);
                setMarginInput('');
                fetchAccount();
            } else {
                const msg = await res.text();
                toast.error(msg || '포지션 오픈에 실패했습니다');
            }
        } catch {
            toast.error('오류가 발생했습니다');
        } finally {
            setLoading(false);
        }
    };

    if (!item) {
        return <div className={classes.loading}>로딩 중...</div>;
    }

    const isUp = item.changeRate != null && item.changeRate >= 0;

    return (
        <div className={classes.page}>
            {/* 가격 헤더 */}
            <div className={classes.priceHeader}>
                <button className={classes.backBtn} onClick={() => navigate('/futures')}>←</button>
                <div>
                    <div className={classes.itemName}>{item.name}</div>
                    <div className={classes.itemMeta}>{item.currency} · {item.priceUnit}</div>
                </div>
                {item.available ? (
                    <div className={classes.priceBlock}>
                        <span className={classes.price}>
                            {item.currentPrice?.toLocaleString('en-US', { maximumFractionDigits: 2 })}
                        </span>
                        <span className={`${classes.change} ${isUp ? classes.rise : classes.fall}`}>
                            {isUp ? '▲' : '▼'} {Math.abs(item.changeRate).toFixed(2)}%
                            ({isUp ? '+' : ''}{item.change?.toFixed(2)})
                        </span>
                    </div>
                ) : (
                    <span className={classes.unavailable}>가격 조회 불가</span>
                )}
            </div>

            {/* 주문 패널 */}
            <div className={classes.orderPanel}>
                {/* 방향 선택 */}
                <div className={classes.section}>
                    <label className={classes.label}>포지션</label>
                    <div className={classes.dirBtns}>
                        <button
                            className={`${classes.dirBtn} ${direction === 'LONG' ? classes.longActive : ''}`}
                            onClick={() => setDirection('LONG')}
                        >
                            롱 (Long) ▲
                        </button>
                        <button
                            className={`${classes.dirBtn} ${direction === 'SHORT' ? classes.shortActive : ''}`}
                            onClick={() => setDirection('SHORT')}
                        >
                            숏 (Short) ▼
                        </button>
                    </div>
                </div>

                {/* 레버리지 선택 */}
                <div className={classes.section}>
                    <label className={classes.label}>레버리지</label>
                    <div className={classes.leverageBtns}>
                        {LEVERAGES.map(lv => (
                            <button
                                key={lv}
                                className={`${classes.levBtn} ${leverage === lv ? classes.levActive : ''}`}
                                onClick={() => setLeverage(lv)}
                            >
                                {lv}x
                            </button>
                        ))}
                    </div>
                </div>

                {/* 증거금 입력 */}
                <div className={classes.section}>
                    <div className={classes.labelRow}>
                        <label className={classes.label}>증거금 (KRW)</label>
                        {isLoggedIn && (
                            <span className={classes.depositHint}>예수금 {deposit.toLocaleString('ko-KR')}원</span>
                        )}
                    </div>
                    <input
                        className={classes.input}
                        type="text"
                        value={marginInput}
                        onChange={handleMarginChange}
                        placeholder="0"
                    />
                    <div className={classes.quickBtns}>
                        {QUICK_PCT.map(pct => (
                            <button key={pct} className={classes.quickBtn} onClick={() => applyPct(pct)}>
                                {pct}%
                            </button>
                        ))}
                    </div>
                </div>

                {/* 요약 */}
                {marginNum > 0 && (
                    <div className={classes.summary}>
                        <div className={classes.summaryRow}>
                            <span>포지션 규모</span>
                            <span>{positionSize.toLocaleString('ko-KR')}원</span>
                        </div>
                        <div className={classes.summaryRow}>
                            <span>투입 증거금</span>
                            <span>{marginNum.toLocaleString('ko-KR')}원</span>
                        </div>
                        <div className={classes.summaryRow}>
                            <span>1% 가격이동 시 손익</span>
                            <span className={direction === 'LONG' ? classes.rise : classes.fall}>
                                {direction === 'LONG' ? '+' : '-'}{Math.round(positionSize * 0.01).toLocaleString('ko-KR')}원
                            </span>
                        </div>
                    </div>
                )}

                <button
                    className={`${classes.openBtn} ${direction === 'LONG' ? classes.longBtn : classes.shortBtn}`}
                    onClick={handleOpen}
                    disabled={loading || !item.available}
                >
                    {loading ? '처리 중...' : `${direction === 'LONG' ? '롱' : '숏'} 진입`}
                </button>

                {!item.available && (
                    <p className={classes.unavailableMsg}>현재 가격 조회가 불가하여 거래할 수 없습니다</p>
                )}
            </div>

            {/* 내 포지션 링크 */}
            <button className={classes.positionsLink} onClick={() => navigate('/futures/positions')}>
                내 선물 포지션 보기 →
            </button>
        </div>
    );
};

export default FuturesDetailPage;

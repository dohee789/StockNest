import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import PendingActionsRoundedIcon from '@mui/icons-material/PendingActionsRounded';
import { useApp } from '../../context/AppContext';
import { API_BASE_URL } from '../../config/api';
import classes from './OrderPage.module.css';

const BASE_URL = API_BASE_URL;

// 지정가 주문 조건 옵션
const CONDITION_OPTIONS = {
    buy: [
        { value: 'PRICE_BELOW', label: '지정가 이하 매수', desc: '현재가가 목표가 이하일 때 자동 매수' },
    ],
    sell: [
        { value: 'PRICE_ABOVE', label: '익절 (목표가 이상)', desc: '현재가가 목표가 이상일 때 자동 매도' },
        { value: 'PRICE_BELOW', label: '손절 (목표가 이하)', desc: '현재가가 목표가 이하일 때 자동 매도' },
    ],
};

const OrderPage = () => {
    const { userId, isLoggedIn } = useApp();
    const navigate = useNavigate();
    const [tab, setTab] = useState('buy');
    const [orderMode, setOrderMode] = useState('market'); // market | limit
    const [watchlist, setWatchlist] = useState([]);
    const [myStocks, setMyStocks] = useState([]);
    const [selectedStock, setSelectedStock] = useState(null);
    const [amount, setAmount] = useState('');
    const [targetPrice, setTargetPrice] = useState('');
    const [conditionType, setConditionType] = useState('PRICE_BELOW');
    const [deposit, setDeposit] = useState(0);
    const [loading, setLoading] = useState(false);

    const fetchData = useCallback(async () => {
        if (!userId) return;
        try {
            const [watchRes, stockRes, accRes] = await Promise.all([
                fetch(`${BASE_URL}/stock-data`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ id: userId }),
                }),
                fetch(`${BASE_URL}/stocks/my-stock`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ customerId: userId }),
                }),
                fetch(`${BASE_URL}/info/account`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ customerId: userId }),
                }),
            ]);
            if (watchRes.ok) setWatchlist(await watchRes.json() || []);
            if (stockRes.ok) setMyStocks(await stockRes.json() || []);
            if (accRes.ok) {
                const acc = await accRes.json();
                setDeposit(acc.deposit || 0);
            }
        } catch (e) {
            console.error(e);
        }
    }, [userId]);

    useEffect(() => { fetchData(); }, [fetchData]);

    useEffect(() => {
        setSelectedStock(null);
        setAmount('');
        setTargetPrice('');
        // 탭 바꾸면 매도 기본 조건 초기화
        setConditionType(tab === 'buy' ? 'PRICE_BELOW' : 'PRICE_ABOVE');
    }, [tab]);

    useEffect(() => {
        setSelectedStock(null);
        setAmount('');
        setTargetPrice('');
    }, [orderMode]);

    const currentStocks = tab === 'buy' ? watchlist : myStocks;

    const nowPrice = tab === 'buy'
        ? (selectedStock?.price ? parseInt(String(selectedStock.price).replace(/,/g, '')) : 0)
        : (selectedStock?.nowPrice || 0);

    const totalPrice = nowPrice * (parseInt(amount) || 0);
    const maxAmount = tab === 'buy'
        ? (nowPrice > 0 ? Math.floor(deposit / nowPrice) : 0)
        : (selectedStock?.amount || 0);

    const isUp = (rate) => rate && (rate.includes('▲') || rate.startsWith('+'));
    const isDown = (rate) => rate && (rate.includes('▼') || rate.startsWith('-'));

    // ── 시장가 주문 ──
    const handleMarketOrder = async () => {
        if (!selectedStock) { toast.error('종목을 선택해주세요'); return; }
        if (nowPrice <= 0) { toast.error('현재가를 불러오는 중입니다'); return; }
        if (!amount || parseInt(amount) <= 0) { toast.error('수량을 입력해주세요'); return; }
        if (tab === 'buy' && totalPrice > deposit) { toast.error('잔액이 부족합니다'); return; }
        if (tab === 'sell' && parseInt(amount) > (selectedStock?.amount || 0)) {
            toast.error('보유 수량보다 많습니다'); return;
        }

        setLoading(true);
        const url = tab === 'buy' ? `${BASE_URL}/stocks/buy-stock` : `${BASE_URL}/stocks/sell-stock`;
        const body = tab === 'buy'
            ? { amount: parseInt(amount), customerId: userId, stockCode: selectedStock.code, name: selectedStock.name, buyPrice: totalPrice }
            : { amount: parseInt(amount), customerId: userId, stockCode: selectedStock.code, sellPrice: totalPrice };

        try {
            const res = await fetch(url, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(body),
            });
            if (res.ok) {
                toast.success(`${selectedStock.name} ${tab === 'buy' ? '매수' : '매도'} 완료!`);
                setSelectedStock(null);
                setAmount('');
                fetchData();
            } else {
                toast.error(await res.text() || '주문 중 오류가 발생했습니다');
            }
        } catch {
            toast.error('주문 중 오류가 발생했습니다');
        } finally {
            setLoading(false);
        }
    };

    // ── 지정가 주문 등록 ──
    const handleLimitOrder = async () => {
        if (!selectedStock) { toast.error('종목을 선택해주세요'); return; }
        if (!amount || parseInt(amount) <= 0) { toast.error('수량을 입력해주세요'); return; }
        const tp = parseInt(String(targetPrice).replace(/,/g, ''));
        if (!tp || tp <= 0) { toast.error('목표가를 입력해주세요'); return; }

        setLoading(true);
        try {
            const res = await fetch(`${BASE_URL}/pending-order`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    customerId: userId,
                    stockCode: selectedStock.code,
                    stockName: selectedStock.name,
                    orderType: tab === 'buy' ? 'BUY' : 'SELL',
                    conditionType,
                    targetPrice: tp,
                    amount: parseInt(amount),
                }),
            });
            if (res.ok) {
                toast.success(`${selectedStock.name} 지정가 주문 등록 완료`);
                setSelectedStock(null);
                setAmount('');
                setTargetPrice('');
            } else {
                toast.error(await res.text() || '주문 등록 중 오류가 발생했습니다');
            }
        } catch {
            toast.error('주문 등록 중 오류가 발생했습니다');
        } finally {
            setLoading(false);
        }
    };

    if (!isLoggedIn) {
        return <div className={classes.notLogin}>로그인이 필요합니다</div>;
    }

    const conditionOptions = CONDITION_OPTIONS[tab];

    return (
        <div className={classes.page}>
            {/* 매수/매도 탭 */}
            <div className={classes.tabs}>
                <button className={`${classes.tabBtn} ${tab === 'buy' ? classes.tabBuy : ''}`} onClick={() => setTab('buy')}>매수</button>
                <button className={`${classes.tabBtn} ${tab === 'sell' ? classes.tabSell : ''}`} onClick={() => setTab('sell')}>매도</button>
            </div>

            {/* 주문 유형 + 대기주문 이동 */}
            <div className={classes.modeBar}>
                <div className={classes.modeBtns}>
                    <button
                        className={`${classes.modeBtn} ${orderMode === 'market' ? classes.modeBtnActive : ''}`}
                        onClick={() => setOrderMode('market')}
                    >
                        시장가
                    </button>
                    <button
                        className={`${classes.modeBtn} ${orderMode === 'limit' ? classes.modeBtnActive : ''}`}
                        onClick={() => setOrderMode('limit')}
                    >
                        지정가
                    </button>
                </div>
                <button className={classes.pendingBtn} onClick={() => navigate('/orders/pending')}>
                    <PendingActionsRoundedIcon fontSize="small" />
                    대기주문
                </button>
            </div>

            {/* 지정가 안내 */}
            {orderMode === 'limit' && (
                <div className={classes.limitNotice}>
                    목표가 도달 시 자동으로 체결됩니다. 30초 주기로 체크합니다.
                </div>
            )}

            {/* 예수금 바 */}
            <div className={`${classes.balanceBar} ${tab === 'buy' ? classes.balanceBuy : classes.balanceSell}`}>
                <span className={classes.balanceLabel}>{tab === 'buy' ? '주문 가능 예수금' : '보유 종목 수'}</span>
                <span className={classes.balanceValue}>
                    {tab === 'buy' ? `${deposit.toLocaleString('ko-KR')}원` : `${myStocks.length}종목`}
                </span>
            </div>

            <div className={classes.content}>
                {/* 지정가 조건 선택 (매도 시) */}
                {orderMode === 'limit' && tab === 'sell' && (
                    <section className={classes.section}>
                        <h3 className={classes.label}>주문 조건</h3>
                        <div className={classes.conditionBtns}>
                            {conditionOptions.map(opt => (
                                <button
                                    key={opt.value}
                                    className={`${classes.conditionBtn} ${conditionType === opt.value ? classes.conditionActive : ''}`}
                                    onClick={() => setConditionType(opt.value)}
                                >
                                    <span className={classes.conditionLabel}>{opt.label}</span>
                                    <span className={classes.conditionDesc}>{opt.desc}</span>
                                </button>
                            ))}
                        </div>
                    </section>
                )}

                {/* 종목 선택 */}
                <section className={classes.section}>
                    <h3 className={classes.label}>종목 선택</h3>
                    {currentStocks.length === 0 ? (
                        <p className={classes.empty}>{tab === 'buy' ? '관심종목을 추가하세요' : '보유 종목이 없습니다'}</p>
                    ) : (
                        <div className={classes.stockCards}>
                            {currentStocks.map(s => {
                                const up = isUp(s.changeRate);
                                const down = isDown(s.changeRate);
                                return (
                                    <button
                                        key={s.code}
                                        className={`${classes.stockCard} ${selectedStock?.code === s.code ? classes.selectedCard : ''}`}
                                        onClick={() => setSelectedStock(s)}
                                    >
                                        <div className={classes.cardLeft}>
                                            <span className={classes.cardName}>{s.name}</span>
                                            <span className={classes.cardCode}>{s.code}</span>
                                        </div>
                                        <div className={classes.cardRight}>
                                            <span className={classes.cardPrice}>
                                                {tab === 'buy' ? `${s.price}원` : `${(s.nowPrice || 0).toLocaleString('ko-KR')}원`}
                                            </span>
                                            <span className={`${classes.cardRate} ${up ? classes.rise : down ? classes.fall : ''}`}>
                                                {tab === 'buy' ? s.changeRate : `${s.amount}주 보유`}
                                            </span>
                                        </div>
                                    </button>
                                );
                            })}
                        </div>
                    )}
                </section>

                {selectedStock && (
                    <>
                        {/* 현재가 카드 */}
                        <div className={classes.priceCard}>
                            <div className={classes.priceInfo}>
                                <span className={classes.stockTitle}>{selectedStock.name}</span>
                                <span className={classes.nowPrice}>{nowPrice.toLocaleString('ko-KR')}원</span>
                                {tab === 'buy' && selectedStock.changeRate && (
                                    <span className={`${classes.priceRate} ${isUp(selectedStock.changeRate) ? classes.rise : classes.fall}`}>
                                        {selectedStock.change} {selectedStock.changeRate}
                                    </span>
                                )}
                            </div>
                            {tab === 'buy' ? (
                                <div className={classes.depositInfo}>
                                    예수금 <strong>{deposit.toLocaleString('ko-KR')}원</strong>
                                    <span className={classes.maxQtyHint}>최대 {maxAmount}주</span>
                                </div>
                            ) : (
                                <div className={classes.depositInfo}>
                                    보유 <strong>{selectedStock.amount}주</strong>
                                    <span className={classes.maxQtyHint}>
                                        평균 {selectedStock.totalBuyPrice > 0
                                            ? Math.round(selectedStock.totalBuyPrice / selectedStock.amount).toLocaleString('ko-KR')
                                            : '-'}원
                                    </span>
                                </div>
                            )}
                        </div>

                        {/* 지정가 목표가 입력 */}
                        {orderMode === 'limit' && (
                            <section className={classes.section}>
                                <h3 className={classes.label}>
                                    목표가
                                    <span className={classes.labelHint}>
                                        {conditionType === 'PRICE_BELOW' ? '(현재가 이하 시 체결)' : '(현재가 이상 시 체결)'}
                                    </span>
                                </h3>
                                <div className={classes.targetPriceRow}>
                                    <input
                                        className={classes.targetPriceInput}
                                        type="number"
                                        placeholder={`목표가 (현재: ${nowPrice.toLocaleString('ko-KR')}원)`}
                                        value={targetPrice}
                                        onChange={e => setTargetPrice(e.target.value)}
                                    />
                                    <span className={classes.wonLabel}>원</span>
                                </div>
                                {nowPrice > 0 && targetPrice && (
                                    <div className={classes.targetHint}>
                                        현재가 대비 <span className={
                                            parseInt(targetPrice) > nowPrice ? classes.rise : classes.fall
                                        }>
                                            {((parseInt(targetPrice) - nowPrice) / nowPrice * 100).toFixed(2)}%
                                        </span>
                                    </div>
                                )}
                            </section>
                        )}

                        {/* 수량 입력 */}
                        <section className={classes.section}>
                            <h3 className={classes.label}>수량</h3>
                            <div className={classes.amountRow}>
                                <button className={classes.amountBtn} onClick={() => setAmount(a => String(Math.max(0, (parseInt(a) || 0) - 1)))}>−</button>
                                <input
                                    className={classes.amountInput}
                                    type="number"
                                    min="1"
                                    value={amount}
                                    onChange={e => setAmount(e.target.value)}
                                    placeholder="0"
                                />
                                <button className={classes.amountBtn} onClick={() => setAmount(a => String((parseInt(a) || 0) + 1))}>+</button>
                            </div>
                            <div className={classes.maxBtns}>
                                {[25, 50, 75, 100].map(pct => (
                                    <button
                                        key={pct}
                                        className={classes.pctBtn}
                                        onClick={() => setAmount(String(Math.floor(maxAmount * pct / 100)))}
                                    >
                                        {pct === 100 ? '최대' : `${pct}%`}
                                    </button>
                                ))}
                            </div>
                        </section>

                        {/* 주문 요약 */}
                        <div className={classes.orderSummary}>
                            <div className={classes.summaryRow}>
                                <span>주문 수량</span>
                                <strong>{(parseInt(amount) || 0).toLocaleString('ko-KR')}주</strong>
                            </div>
                            <div className={classes.summaryDivider} />
                            {orderMode === 'market' ? (
                                <div className={classes.summaryRow}>
                                    <span>주문 금액</span>
                                    <strong className={tab === 'buy' ? classes.rise : classes.fall}>
                                        {totalPrice.toLocaleString('ko-KR')}원
                                    </strong>
                                </div>
                            ) : (
                                <div className={classes.summaryRow}>
                                    <span>목표가</span>
                                    <strong className={classes.accent}>
                                        {parseInt(targetPrice) > 0 ? parseInt(targetPrice).toLocaleString('ko-KR') : '-'}원
                                    </strong>
                                </div>
                            )}
                        </div>

                        {orderMode === 'market' ? (
                            <button
                                className={`${classes.orderBtn} ${tab === 'buy' ? classes.buyBtn : classes.sellBtn}`}
                                onClick={handleMarketOrder}
                                disabled={loading || nowPrice <= 0}
                            >
                                {loading ? '처리 중...' : `${tab === 'buy' ? '매수' : '매도'} 주문`}
                            </button>
                        ) : (
                            <button
                                className={`${classes.orderBtn} ${classes.limitBtn}`}
                                onClick={handleLimitOrder}
                                disabled={loading}
                            >
                                {loading ? '등록 중...' : '지정가 주문 등록'}
                            </button>
                        )}
                    </>
                )}
            </div>
        </div>
    );
};

export default OrderPage;

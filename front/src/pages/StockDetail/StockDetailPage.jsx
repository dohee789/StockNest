import React, { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useApp } from '../../context/AppContext';
import toast from 'react-hot-toast';
import ArrowBackRoundedIcon from '@mui/icons-material/ArrowBackRounded';
import StarRoundedIcon from '@mui/icons-material/StarRounded';
import StarBorderRoundedIcon from '@mui/icons-material/StarBorderRounded';
import TrendingUpRoundedIcon from '@mui/icons-material/TrendingUpRounded';
import TrendingDownRoundedIcon from '@mui/icons-material/TrendingDownRounded';
import TrendingFlatRoundedIcon from '@mui/icons-material/TrendingFlatRounded';
import AutoAwesomeRoundedIcon from '@mui/icons-material/AutoAwesomeRounded';
import Markdown from 'markdown-to-jsx';
import CandleChart from './CandleChart';
import { API_BASE_URL } from '../../config/api';
import classes from './StockDetailPage.module.css';

const BASE_URL = API_BASE_URL;
const TIMEFRAMES = [
    { label: '일봉', value: 'day' },
    { label: '주봉', value: 'week' },
    { label: '월봉', value: 'month' },
];

const StockDetailPage = () => {
    const { code } = useParams();
    const navigate = useNavigate();
    const { userId, isLoggedIn, realtimePrices } = useApp();

    const [detail, setDetail] = useState(null);
    const [loading, setLoading] = useState(true);
    const [timeframe, setTimeframe] = useState('day');
    const [candleCache, setCandleCache] = useState({});
    const [candleLoading, setCandleLoading] = useState(false);
    const [inWatchlist, setInWatchlist] = useState(false);

    // 주문 상태
    const [orderTab, setOrderTab] = useState('BUY');
    const [quantity, setQuantity] = useState(1);
    const [accountInfo, setAccountInfo] = useState(null);
    const [myStock, setMyStock] = useState(null); // 현재 종목 보유 정보
    const [orderLoading, setOrderLoading] = useState(false);
    const [confirmOpen, setConfirmOpen] = useState(false);

    // AI 분석
    const [aiAnalysis, setAiAnalysis] = useState('');
    const [aiLoading, setAiLoading] = useState(false);
    const [aiOpen, setAiOpen] = useState(false);

    // 종목 상세 가져오기 (첫 로드: 일봉 포함)
    const fetchDetail = useCallback(async () => {
        setLoading(true);
        try {
            const res = await fetch(`${BASE_URL}/stock-detail/${code}?timeframe=day`);
            if (res.ok) {
                const data = await res.json();
                setDetail(data);
                setCandleCache(prev => ({ ...prev, day: data.candles || [] }));
            }
        } catch (e) {
            toast.error('종목 정보를 불러오지 못했습니다');
        } finally {
            setLoading(false);
        }
    }, [code]);

    // 봉 전환 시 캔들 데이터 가져오기
    const fetchCandles = useCallback(async (tf) => {
        if (candleCache[tf]) { setTimeframe(tf); return; }
        setCandleLoading(true);
        try {
            const res = await fetch(`${BASE_URL}/stock-detail/${code}?timeframe=${tf}`);
            if (res.ok) {
                const data = await res.json();
                setCandleCache(prev => ({ ...prev, [tf]: data.candles || [] }));
            }
        } catch { /* ignore */ }
        setTimeframe(tf);
        setCandleLoading(false);
    }, [code, candleCache]);

    // 계좌 + 보유 정보 가져오기
    const fetchAccount = useCallback(async () => {
        if (!isLoggedIn) return;
        try {
            const [accRes, stocksRes] = await Promise.all([
                fetch(`${BASE_URL}/info/account`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ customerId: userId }),
                }),
                fetch(`${BASE_URL}/stocks/my-stock`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ customerId: userId }),
                }),
            ]);
            if (accRes.ok) setAccountInfo(await accRes.json());
            if (stocksRes.ok) {
                const allStocks = await stocksRes.json();
                const found = (allStocks || []).find(s => s.stockCode === code);
                setMyStock(found || null);
            }
        } catch { /* ignore */ }
    }, [userId, isLoggedIn, code]);

    useEffect(() => {
        fetchDetail();
        fetchAccount();
    }, [fetchDetail, fetchAccount]);

    // 실시간 가격 반영
    const rt = realtimePrices[code];
    const livePrice = rt?.price || detail?.price || '-';
    const liveChange = rt?.change || detail?.change || '-';
    const liveChangeRate = rt?.changeRate || detail?.changeRate || '-';

    const isRise = liveChangeRate.startsWith('▲') || liveChangeRate.startsWith('+');
    const isFall = liveChangeRate.startsWith('▼') || liveChangeRate.startsWith('-');
    const priceClass = isRise ? classes.rise : isFall ? classes.fall : classes.flat;

    // 현재 주가 (숫자)
    const priceNum = parseInt((livePrice || '0').replace(/,/g, ''), 10) || 0;

    // 현재 timeframe 캔들
    const displayCandles = candleCache[timeframe] || [];

    // 관심종목 토글
    const handleToggleWatch = async () => {
        if (!isLoggedIn) { toast.error('로그인이 필요합니다'); return; }
        if (inWatchlist) {
            await fetch(`${BASE_URL}/stocks/watch`, {
                method: 'DELETE',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ code }),
            });
            setInWatchlist(false);
            toast.success('관심종목 해제');
        } else {
            const res = await fetch(`${BASE_URL}/stocks/watch`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ code, name: detail?.name, customerId: userId }),
            });
            if (res.ok) { setInWatchlist(true); toast.success('관심종목 추가'); }
            else toast.error('이미 추가된 종목입니다');
        }
    };

    // 주문 유효성 검사 (확인 모달 열기 전)
    const handleOrderClick = () => {
        if (!isLoggedIn) { toast.error('로그인이 필요합니다'); return; }
        if (!accountInfo?.id) { toast.error('계좌가 없습니다. MY 페이지에서 계좌를 만들어주세요'); return; }
        if (priceNum <= 0) { toast.error('현재가를 불러오는 중입니다. 잠시 후 다시 시도해주세요'); return; }
        if (quantity < 1) { toast.error('수량을 입력해주세요'); return; }
        if (orderTab === 'BUY' && accountInfo.deposit < priceNum * quantity) {
            toast.error('잔액이 부족합니다'); return;
        }
        setConfirmOpen(true);
    };

    // 실제 주문 처리
    const handleOrder = async () => {
        setConfirmOpen(false);
        const totalPrice = priceNum * quantity;
        setOrderLoading(true);
        try {
            if (orderTab === 'BUY') {
                const res = await fetch(`${BASE_URL}/stocks/buy-stock`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        customerId: userId,
                        stockCode: code,
                        name: detail?.name,
                        amount: quantity,
                        buyPrice: totalPrice,
                    }),
                });
                if (res.ok) {
                    toast.success(`${detail?.name} ${quantity}주 매수 완료`);
                    setQuantity(1);
                    fetchAccount();
                } else {
                    const errText = await res.text();
                    try { toast.error(JSON.parse(errText).message || '매수에 실패했습니다'); }
                    catch { toast.error(errText || '매수에 실패했습니다'); }
                }
            } else {
                const res = await fetch(`${BASE_URL}/stocks/sell-stock`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        customerId: userId,
                        stockCode: code,
                        amount: quantity,
                        sellPrice: totalPrice,
                    }),
                });
                if (res.ok) {
                    toast.success(`${detail?.name} ${quantity}주 매도 완료`);
                    setQuantity(1);
                    fetchAccount();
                } else {
                    const errText = await res.text();
                    try { toast.error(JSON.parse(errText).message || '매도에 실패했습니다'); }
                    catch { toast.error(errText || '매도에 실패했습니다'); }
                }
            }
        } finally {
            setOrderLoading(false);
        }
    };

    const handleAiAnalysis = async () => {
        if (aiLoading) return;
        setAiOpen(true);
        setAiLoading(true);
        setAiAnalysis('');
        try {
            const res = await fetch(`${BASE_URL}/chatbot/stock-analysis`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ customerId: userId || '', stockCode: code }),
            });
            if (res.ok) {
                const data = await res.json();
                setAiAnalysis(data.analysis);
            } else {
                setAiAnalysis('분석을 불러올 수 없습니다.');
            }
        } catch {
            setAiAnalysis('서버 연결에 실패했습니다.');
        } finally {
            setAiLoading(false);
        }
    };

    if (loading) {
        return (
            <div className={classes.loadingPage}>
                <div className={classes.spinner} />
                <p>시세 불러오는 중...</p>
            </div>
        );
    }

    return (
        <div className={classes.page}>
            {/* 상단 헤더 */}
            <div className={classes.topBar}>
                <button className={classes.backBtn} onClick={() => navigate(-1)}>
                    <ArrowBackRoundedIcon />
                </button>
                <div className={classes.titleArea}>
                    <h1 className={classes.stockName}>{detail?.name || code}</h1>
                    <span className={classes.stockCode}>{code}</span>
                </div>
                <button className={classes.watchBtn} onClick={handleToggleWatch}>
                    {inWatchlist
                        ? <StarRoundedIcon className={classes.starActive} />
                        : <StarBorderRoundedIcon />}
                </button>
            </div>

            {/* 가격 영역 */}
            <div className={classes.priceSection}>
                <div className={`${classes.currentPrice} ${priceClass}`}>
                    {livePrice}원
                </div>
                <div className={`${classes.priceChange} ${priceClass}`}>
                    {isRise ? <TrendingUpRoundedIcon fontSize="small" />
                        : isFall ? <TrendingDownRoundedIcon fontSize="small" />
                            : <TrendingFlatRoundedIcon fontSize="small" />}
                    <span>{liveChange}</span>
                    <span className={classes.changeRate}>{liveChangeRate}</span>
                </div>
            </div>

            {/* 추가 정보 */}
            {(detail?.volume || detail?.high52w || detail?.per) && (
                <div className={classes.statsRow}>
                    {detail.volume && (
                        <div className={classes.statItem}>
                            <span className={classes.statLabel}>거래량</span>
                            <span className={classes.statValue}>{detail.volume}</span>
                        </div>
                    )}
                    {detail.high52w && (
                        <div className={classes.statItem}>
                            <span className={classes.statLabel}>52주 최고</span>
                            <span className={`${classes.statValue} ${classes.rise}`}>{detail.high52w}</span>
                        </div>
                    )}
                    {detail.low52w && (
                        <div className={classes.statItem}>
                            <span className={classes.statLabel}>52주 최저</span>
                            <span className={`${classes.statValue} ${classes.fall}`}>{detail.low52w}</span>
                        </div>
                    )}
                    {detail.per && (
                        <div className={classes.statItem}>
                            <span className={classes.statLabel}>PER</span>
                            <span className={classes.statValue}>{detail.per}</span>
                        </div>
                    )}
                    {detail.pbr && (
                        <div className={classes.statItem}>
                            <span className={classes.statLabel}>PBR</span>
                            <span className={classes.statValue}>{detail.pbr}</span>
                        </div>
                    )}
                </div>
            )}

            {/* 데스크탑: 차트 + 주문 2열 */}
            <div className={classes.mainGrid}>
                {/* 차트 영역 */}
                <div className={classes.chartSection}>
                    <div className={classes.periodSelector}>
                        {TIMEFRAMES.map(({ label, value }) => (
                            <button
                                key={value}
                                className={`${classes.periodBtn} ${timeframe === value ? classes.periodActive : ''}`}
                                onClick={() => fetchCandles(value)}
                            >
                                {label}
                            </button>
                        ))}
                    </div>
                    {candleLoading ? (
                        <div className={classes.noChart}>불러오는 중...</div>
                    ) : displayCandles.length > 0 ? (
                        <CandleChart candles={displayCandles} />
                    ) : (
                        <div className={classes.noChart}>차트 데이터를 불러올 수 없습니다</div>
                    )}
                </div>

                {/* 주문 패널 */}
                <div className={classes.orderPanel}>
                    <div className={classes.orderTabs}>
                        <button
                            className={`${classes.orderTab} ${orderTab === 'BUY' ? classes.buyActive : ''}`}
                            onClick={() => setOrderTab('BUY')}
                        >
                            매수
                        </button>
                        <button
                            className={`${classes.orderTab} ${orderTab === 'SELL' ? classes.sellActive : ''}`}
                            onClick={() => setOrderTab('SELL')}
                        >
                            매도
                        </button>
                    </div>

                    <div className={classes.orderBody}>
                        {/* 계좌 정보 */}
                        {isLoggedIn && accountInfo?.id && (
                            <div className={classes.accountInfo}>
                                {orderTab === 'BUY' ? (
                                    <div className={classes.infoRow}>
                                        <span>주문 가능</span>
                                        <span className={classes.infoValue}>
                                            {(accountInfo.deposit || 0).toLocaleString()}원
                                        </span>
                                    </div>
                                ) : (
                                    <>
                                        <div className={classes.infoRow}>
                                            <span>보유수량</span>
                                            <span className={classes.infoValue}>
                                                {myStock ? `${myStock.amount}주` : '미보유'}
                                            </span>
                                        </div>
                                        {myStock && (
                                            <div className={classes.infoRow}>
                                                <span>평균단가</span>
                                                <span className={classes.infoValue}>
                                                    {Math.round(myStock.totalBuyPrice / myStock.amount).toLocaleString()}원
                                                </span>
                                            </div>
                                        )}
                                    </>
                                )}
                            </div>
                        )}

                        {/* 현재가 */}
                        <div className={classes.infoRow}>
                            <span>현재가</span>
                            <span className={`${classes.infoValue} ${priceClass}`}>{livePrice}원</span>
                        </div>

                        {/* 수량 입력 */}
                        <div className={classes.qtyRow}>
                            <span className={classes.qtyLabel}>수량</span>
                            <div className={classes.qtyControl}>
                                <button
                                    className={classes.qtyBtn}
                                    onClick={() => setQuantity(q => Math.max(1, q - 1))}
                                >−</button>
                                <input
                                    type="number"
                                    className={classes.qtyInput}
                                    value={quantity}
                                    min={1}
                                    onChange={e => setQuantity(Math.max(1, parseInt(e.target.value) || 1))}
                                />
                                <button
                                    className={classes.qtyBtn}
                                    onClick={() => setQuantity(q => q + 1)}
                                >+</button>
                            </div>
                        </div>

                        {/* 빠른 수량 선택 */}
                        {isLoggedIn && (
                            <div className={classes.quickBtns}>
                                {orderTab === 'BUY' && accountInfo?.deposit > 0
                                    ? [10, 25, 50, 100].map(pct => (
                                        <button
                                            key={pct}
                                            className={classes.quickBtn}
                                            onClick={() => {
                                                const maxQty = Math.floor((accountInfo.deposit * pct / 100) / priceNum);
                                                setQuantity(Math.max(1, maxQty));
                                            }}
                                        >
                                            {pct}%
                                        </button>
                                    ))
                                    : myStock && [25, 50, 100].map(pct => (
                                        <button
                                            key={pct}
                                            className={classes.quickBtn}
                                            onClick={() => {
                                                const qty = Math.max(1, Math.floor(myStock.amount * pct / 100));
                                                setQuantity(qty);
                                            }}
                                        >
                                            {pct === 100 ? '전량' : `${pct}%`}
                                        </button>
                                    ))
                                }
                            </div>
                        )}

                        {/* 주문 총액 */}
                        <div className={classes.totalRow}>
                            <span>주문금액</span>
                            <span className={classes.totalValue}>
                                {(priceNum * quantity).toLocaleString()}원
                            </span>
                        </div>

                        {/* 주문 버튼 */}
                        <button
                            className={`${classes.submitBtn} ${orderTab === 'BUY' ? classes.buyBtn : classes.sellBtn}`}
                            onClick={handleOrderClick}
                            disabled={orderLoading}
                        >
                            {orderLoading ? '처리 중...'
                                : orderTab === 'BUY'
                                    ? `${detail?.name || ''} 매수`
                                    : `${detail?.name || ''} 매도`}
                        </button>

                        {!isLoggedIn && (
                            <p className={classes.loginHint}>로그인 후 거래할 수 있습니다</p>
                        )}
                    </div>
                </div>
            </div>

            {/* AI 분석 */}
            <div className={classes.aiSection}>
                <button className={classes.aiBtn} onClick={handleAiAnalysis} disabled={aiLoading}>
                    <AutoAwesomeRoundedIcon style={{ fontSize: 18 }} />
                    {aiLoading ? 'AI 분석 중...' : 'AI 종목 분석'}
                </button>
                {aiOpen && (
                    <div className={classes.aiResult}>
                        {aiLoading ? (
                            <div className={classes.aiLoading}>
                                <div className={classes.spinner} />
                                <p>AI가 종목을 분석하고 있습니다...</p>
                            </div>
                        ) : (
                            <div className={classes.aiContent}>
                                <Markdown>{aiAnalysis}</Markdown>
                            </div>
                        )}
                    </div>
                )}
            </div>

            {/* 주문 확인 모달 */}
            {confirmOpen && (
                <div className={classes.modalOverlay} onClick={() => setConfirmOpen(false)}>
                    <div className={classes.modalBox} onClick={e => e.stopPropagation()}>
                        <h3 className={classes.modalTitle}>
                            {orderTab === 'BUY' ? '매수 확인' : '매도 확인'}
                        </h3>
                        <div className={classes.modalRow}>
                            <span>종목</span>
                            <strong>{detail?.name}</strong>
                        </div>
                        <div className={classes.modalRow}>
                            <span>수량</span>
                            <strong>{quantity.toLocaleString()}주</strong>
                        </div>
                        <div className={classes.modalRow}>
                            <span>주문단가</span>
                            <strong className={priceClass}>{livePrice}원</strong>
                        </div>
                        <div className={`${classes.modalRow} ${classes.modalTotal}`}>
                            <span>주문금액</span>
                            <strong>{(priceNum * quantity).toLocaleString()}원</strong>
                        </div>
                        <div className={classes.modalActions}>
                            <button className={classes.modalCancel} onClick={() => setConfirmOpen(false)}>
                                취소
                            </button>
                            <button
                                className={`${classes.modalConfirm} ${orderTab === 'BUY' ? classes.buyBtn : classes.sellBtn}`}
                                onClick={handleOrder}
                            >
                                {orderTab === 'BUY' ? '매수' : '매도'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default StockDetailPage;

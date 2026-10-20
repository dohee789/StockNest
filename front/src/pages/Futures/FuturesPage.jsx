import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { API_BASE_URL } from '../../config/api';
import classes from './FuturesPage.module.css';

const BASE_URL = API_BASE_URL;

const CATEGORY_LABEL = { INDEX: '지수', COMMODITY: '원자재' };

const FuturesPage = () => {
    const navigate = useNavigate();
    const [futures, setFutures] = useState([]);
    const [loading, setLoading] = useState(true);
    const [activeTab, setActiveTab] = useState('ALL');

    const fetchFutures = async () => {
        try {
            const res = await fetch(`${BASE_URL}/futures`);
            if (res.ok) setFutures(await res.json());
        } catch { /* ignore */ } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchFutures();
        const id = setInterval(fetchFutures, 30000);
        return () => clearInterval(id);
    }, []);

    const tabs = ['ALL', 'INDEX', 'COMMODITY'];
    const filtered = activeTab === 'ALL' ? futures : futures.filter(f => f.category === activeTab);

    return (
        <div className={classes.page}>
            <div className={classes.header}>
                <h2 className={classes.title}>선물 거래</h2>
                <p className={classes.desc}>레버리지를 활용한 지수·원자재 선물 모의투자</p>
            </div>

            <div className={classes.tabs}>
                {tabs.map(t => (
                    <button
                        key={t}
                        className={`${classes.tab} ${activeTab === t ? classes.activeTab : ''}`}
                        onClick={() => setActiveTab(t)}
                    >
                        {t === 'ALL' ? '전체' : CATEGORY_LABEL[t]}
                    </button>
                ))}
            </div>

            {loading ? (
                <div className={classes.loading}>가격 조회 중...</div>
            ) : (
                <div className={classes.list}>
                    {filtered.map(f => (
                        <FuturesCard key={f.code} item={f} onClick={() => navigate(`/futures/${f.code}`)} />
                    ))}
                </div>
            )}

            <div className={classes.infoBox}>
                <p className={classes.infoTitle}>선물 거래 안내</p>
                <ul className={classes.infoList}>
                    <li>증거금(margin)만으로 더 큰 포지션을 제어합니다</li>
                    <li>롱(Long): 가격 상승 시 수익 · 숏(Short): 가격 하락 시 수익</li>
                    <li>레버리지 배율만큼 손익이 증폭됩니다</li>
                    <li>증거금 전액 손실 시 강제청산(로스컷)됩니다</li>
                </ul>
            </div>
        </div>
    );
};

const FuturesCard = ({ item, onClick }) => {
    const isUp = item.changeRate != null && item.changeRate >= 0;
    const changeClass = isUp ? classes.rise : classes.fall;

    const formatPrice = (price, currency, unit) => {
        if (price == null) return '-';
        if (currency === 'KRW') return price.toLocaleString('ko-KR', { maximumFractionDigits: 2 });
        return price.toLocaleString('en-US', { maximumFractionDigits: 2 });
    };

    return (
        <button className={classes.card} onClick={onClick}>
            <div className={classes.cardLeft}>
                <span className={classes.categoryTag}>{CATEGORY_LABEL[item.category] || item.category}</span>
                <span className={classes.cardName}>{item.name}</span>
                <span className={classes.cardUnit}>{item.currency} · {item.priceUnit}</span>
            </div>
            <div className={classes.cardRight}>
                {item.available ? (
                    <>
                        <span className={classes.cardPrice}>
                            {formatPrice(item.currentPrice, item.currency, item.priceUnit)}
                        </span>
                        <span className={`${classes.cardChange} ${changeClass}`}>
                            {isUp ? '▲' : '▼'} {Math.abs(item.changeRate).toFixed(2)}%
                        </span>
                    </>
                ) : (
                    <span className={classes.unavailable}>조회 불가</span>
                )}
            </div>
        </button>
    );
};

export default FuturesPage;

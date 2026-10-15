import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import classes from './EtfPage.module.css';

const BASE_URL = 'http://localhost:8090';

const CATEGORY_LABEL = {
    DOMESTIC_INDEX: '국내지수',
    FOREIGN_INDEX: '해외지수',
    SECTOR: '섹터/테마',
    COMMODITY: '원자재',
    BOND: '채권',
    LEVERAGE: '레버리지/인버스',
};

const TABS = ['ALL', 'DOMESTIC_INDEX', 'FOREIGN_INDEX', 'SECTOR', 'COMMODITY', 'BOND', 'LEVERAGE'];

const EtfPage = () => {
    const navigate = useNavigate();
    const [etfs, setEtfs] = useState([]);
    const [loading, setLoading] = useState(true);
    const [activeTab, setActiveTab] = useState('ALL');

    const fetchEtfs = async () => {
        try {
            const res = await fetch(`${BASE_URL}/etf`);
            if (res.ok) setEtfs(await res.json());
        } catch { /* ignore */ } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchEtfs();
        const id = setInterval(fetchEtfs, 30000);
        return () => clearInterval(id);
    }, []);

    const filtered = activeTab === 'ALL' ? etfs : etfs.filter(e => e.category === activeTab);

    return (
        <div className={classes.page}>
            <div className={classes.header}>
                <h2 className={classes.title}>ETF</h2>
                <p className={classes.desc}>국내·해외 지수, 섹터, 원자재, 채권 등 다양한 ETF</p>
            </div>

            <div className={classes.tabs}>
                {TABS.map(t => (
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
                    {filtered.map(e => (
                        <EtfCard
                            key={e.code}
                            item={e}
                            onClick={() => navigate(`/stock/${e.code}`)}
                        />
                    ))}
                </div>
            )}

            <div className={classes.infoBox}>
                <p className={classes.infoTitle}>ETF 안내</p>
                <ul className={classes.infoList}>
                    <li>ETF는 주식처럼 실시간 매매가 가능합니다</li>
                    <li>지수·섹터·원자재 등을 한 종목으로 분산 투자할 수 있습니다</li>
                    <li>레버리지/인버스 ETF는 변동성이 크므로 주의가 필요합니다</li>
                    <li>클릭하면 차트 조회 및 매수/매도가 가능합니다</li>
                </ul>
            </div>
        </div>
    );
};

const EtfCard = ({ item, onClick }) => {
    const isUp = item.changeRate != null && item.changeRate >= 0;
    const changeClass = isUp ? classes.rise : classes.fall;

    return (
        <button className={classes.card} onClick={onClick}>
            <div className={classes.cardLeft}>
                <div className={classes.cardTopRow}>
                    <span className={classes.categoryTag}>
                        {CATEGORY_LABEL[item.category] || item.category}
                    </span>
                    <span className={classes.trackingIndex}>{item.trackingIndex}</span>
                </div>
                <span className={classes.cardName}>{item.name}</span>
                <span className={classes.cardCode}>{item.code}</span>
            </div>
            <div className={classes.cardRight}>
                {item.available ? (
                    <>
                        <span className={classes.cardPrice}>
                            {item.currentPrice?.toLocaleString('ko-KR', { maximumFractionDigits: 0 })}
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

export default EtfPage;

import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { IconSearch, IconStar, IconChevronRight, IconFlame, IconTrendingUp, IconTrendingDown, IconChartBar } from '@tabler/icons-react';
import toast from 'react-hot-toast';
import { useApp } from '../../context/AppContext';
import { API_BASE_URL } from '../../config/api';
import classes from './StocksPage.module.css';

const BASE_URL = API_BASE_URL;

const POPULAR = [
    { code: '005930', name: '삼성전자' },
    { code: '000660', name: 'SK하이닉스' },
    { code: '373220', name: 'LG에너지솔루션' },
    { code: '207940', name: '삼성바이오로직스' },
    { code: '005380', name: '현대차' },
    { code: '035420', name: 'NAVER' },
    { code: '000270', name: '기아' },
    { code: '068270', name: '셀트리온' },
    { code: '051910', name: 'LG화학' },
    { code: '035720', name: '카카오' },
    { code: '006400', name: '삼성SDI' },
    { code: '105560', name: 'KB금융' },
    { code: '055550', name: '신한지주' },
    { code: '012330', name: '현대모비스' },
    { code: '034020', name: '두산에너빌리티' },
    { code: '003670', name: '포스코퓨처엠' },
    { code: '005490', name: 'POSCO홀딩스' },
    { code: '096770', name: 'SK이노베이션' },
    { code: '066570', name: 'LG전자' },
    { code: '028260', name: '삼성물산' },
];

const TABS = [
    { key: 'popular', label: '인기', Icon: IconFlame },
    { key: 'rising', label: '급등', Icon: IconTrendingUp },
    { key: 'falling', label: '급락', Icon: IconTrendingDown },
    { key: 'volume', label: '거래량', Icon: IconChartBar },
];

const StocksPage = () => {
    const navigate = useNavigate();
    const { userId, isLoggedIn } = useApp();
    const [query, setQuery] = useState('');
    const [results, setResults] = useState([]);
    const [searching, setSearching] = useState(false);
    const [activeTab, setActiveTab] = useState('popular');
    const [rankData, setRankData] = useState({});
    const [rankLoading, setRankLoading] = useState(false);
    const debounceRef = useRef(null);

    useEffect(() => {
        if (activeTab === 'popular') return;
        if (rankData[activeTab]) return;
        const url = {
            rising: `${BASE_URL}/sise/rising`,
            falling: `${BASE_URL}/sise/falling`,
            volume: `${BASE_URL}/sise/volume`,
        }[activeTab];
        if (!url) return;
        setRankLoading(true);
        fetch(url)
            .then(r => r.ok ? r.json() : [])
            .then(data => setRankData(prev => ({ ...prev, [activeTab]: data })))
            .catch(() => {})
            .finally(() => setRankLoading(false));
    }, [activeTab, rankData]);

    const doSearch = useCallback(async (q) => {
        if (!q.trim()) { setResults([]); return; }
        setSearching(true);
        try {
            const res = await fetch(`${BASE_URL}/stocks`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ name: q }),
            });
            if (res.ok) setResults(await res.json() || []);
        } catch {
            toast.error('검색 중 오류가 발생했습니다');
        } finally {
            setSearching(false);
        }
    }, []);

    const handleChange = (e) => {
        const val = e.target.value;
        setQuery(val);
        if (debounceRef.current) clearTimeout(debounceRef.current);
        debounceRef.current = setTimeout(() => doSearch(val), 300);
    };

    const handleAddWatch = async (e, stock) => {
        e.stopPropagation();
        if (!isLoggedIn) { toast.error('로그인이 필요합니다'); return; }
        try {
            const res = await fetch(`${BASE_URL}/stocks/watch`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ code: stock.code, name: stock.name, customerId: userId }),
            });
            if (res.ok) toast.success(`${stock.name} 관심종목 추가`);
            else toast.error('이미 추가된 종목입니다');
        } catch {
            toast.error('추가 중 오류');
        }
    };

    const goToDetail = (code) => navigate(`/stock/${code}`);

    const isUp = (rate) => rate && (rate.includes('▲') || rate.startsWith('+'));
    const isDown = (rate) => rate && (rate.includes('▼') || rate.startsWith('-'));

    const currentList = activeTab === 'popular' ? null : (rankData[activeTab] || []);

    return (
        <div className={classes.page}>
            {/* 검색창 */}
            <div className={classes.searchSection}>
                <div className={classes.searchBox}>
                    <IconSearch size={18} stroke={1.5} className={classes.searchIcon} />
                    <input
                        className={classes.input}
                        placeholder="종목명 또는 코드 검색"
                        value={query}
                        onChange={handleChange}
                    />
                    {searching && <span className={classes.searchingText}>검색 중...</span>}
                </div>
            </div>

            {/* 검색 결과 */}
            {results.length > 0 && (
                <div className={classes.section}>
                    <div className={classes.sectionHeader}>
                        <h2 className={classes.sectionTitle}>검색 결과</h2>
                        <span className={classes.sectionCount}>{results.length}개</span>
                        <button className={classes.clearBtn} onClick={() => { setResults([]); setQuery(''); }}>
                            닫기
                        </button>
                    </div>
                    <div className={classes.stockList}>
                        {results.map(s => (
                            <div key={s.code} className={classes.stockItem} onClick={() => goToDetail(s.code)}>
                                <div className={classes.stockInfo}>
                                    <span className={classes.stockName}>{s.name}</span>
                                    <span className={classes.stockCode}>{s.code}</span>
                                </div>
                                <div className={classes.itemActions}>
                                    <button
                                        className={classes.watchBtn}
                                        onClick={(e) => handleAddWatch(e, s)}
                                        title="관심종목 추가"
                                    >
                                        <IconStar size={16} stroke={1.5} />
                                    </button>
                                    <IconChevronRight size={16} stroke={1.5} className={classes.chevron} />
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            )}

            {/* 탭 */}
            {results.length === 0 && (
                <div className={classes.section}>
                    <div className={classes.tabs}>
                        {TABS.map(tab => (
                            <button
                                key={tab.key}
                                className={`${classes.tab} ${activeTab === tab.key ? classes.tabActive : ''}`}
                                onClick={() => setActiveTab(tab.key)}
                            >
                                <tab.Icon size={16} stroke={1.5} />
                                <span>{tab.label}</span>
                            </button>
                        ))}
                    </div>

                    {/* 인기 종목 */}
                    {activeTab === 'popular' && (
                        <div className={classes.stockList}>
                            {POPULAR.map((s, i) => (
                                <div key={s.code} className={classes.stockItem} onClick={() => goToDetail(s.code)}>
                                    <div className={classes.stockInfo}>
                                        <span className={classes.rankBadge}>{i + 1}</span>
                                        <div>
                                            <span className={classes.stockName}>{s.name}</span>
                                            <span className={classes.stockCode}>{s.code}</span>
                                        </div>
                                    </div>
                                    <div className={classes.itemActions}>
                                        <button
                                            className={classes.watchBtn}
                                            onClick={(e) => handleAddWatch(e, s)}
                                            title="관심종목 추가"
                                        >
                                            <IconStar size={16} stroke={1.5} />
                                        </button>
                                        <IconChevronRight size={16} stroke={1.5} className={classes.chevron} />
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}

                    {/* 랭킹 탭 (급등/급락/거래량) */}
                    {activeTab !== 'popular' && (
                        rankLoading ? (
                            <div className={classes.rankLoading}>
                                {[1,2,3,4,5].map(i => <div key={i} className={classes.skeletonRow} />)}
                            </div>
                        ) : (
                            <div className={classes.stockList}>
                                {(currentList || []).map((s, i) => (
                                    <div key={s.code || i} className={classes.stockItem} onClick={() => goToDetail(s.code)}>
                                        <div className={classes.stockInfo}>
                                            <span className={classes.rankBadge}>{i + 1}</span>
                                            <div>
                                                <span className={classes.stockName}>{s.name}</span>
                                                <span className={classes.stockCode}>{s.code}</span>
                                            </div>
                                        </div>
                                        <div className={classes.rankRight}>
                                            <span className={classes.rankPrice}>{s.price}원</span>
                                            <span className={`${classes.rankRate} ${isUp(s.changeRate) ? classes.rise : isDown(s.changeRate) ? classes.fall : ''}`}>
                                                {s.changeRate}
                                            </span>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )
                    )}
                </div>
            )}
        </div>
    );
};

export default StocksPage;

import React, { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useApp } from '../../context/AppContext';
import { PieChart, Pie, Cell, Tooltip, ResponsiveContainer } from 'recharts';
import { IconTrendingUp, IconTrendingDown, IconCreditCardFilled, IconSparkles, IconX, IconEgg, IconEggCracked } from '@tabler/icons-react';
import { IconChick, IconChicken } from '../../components/icons/GrowthIcons';
import Markdown from 'markdown-to-jsx';
import toast from 'react-hot-toast';
import { API_BASE_URL } from '../../config/api';
import classes from './PortfolioPage.module.css';

const BASE_URL = API_BASE_URL;

const PIE_COLORS = ['#EAB308', '#F59E0B', '#D97706', '#B45309', '#92400E', '#78716C', '#A8A29E', '#CA8A04', '#FACC15', '#FDE68A'];

const GROWTH_STAGES = [
    { min: -Infinity, max: 0,   Icon: IconEgg, label: '알 단계', desc: '아직은 알이에요. 화이팅!' },
    { min: 0,         max: 10,  Icon: IconEggCracked, label: '부화 단계', desc: '알을 깨고 나왔어요!' },
    { min: 10,        max: 50,  Icon: IconChick, label: '병아리 단계', desc: '쑥쑥 크는 중이에요!' },
    { min: 50,        max: Infinity, Icon: IconChicken, label: '닭 단계', desc: '어엿한 닭으로 성장했어요!' },
];

const SEED_OPTIONS = [
    { label: '100만', value: 1000000 },
    { label: '500만', value: 5000000 },
    { label: '1,000만', value: 10000000 },
    { label: '5,000만', value: 50000000 },
];

const PortfolioPage = () => {
    const navigate = useNavigate();
    const { userId, isLoggedIn } = useApp();
    const [stocks, setStocks] = useState([]);
    const [account, setAccount] = useState(null);
    const [loading, setLoading] = useState(false);

    // 계좌 생성 폼 상태
    const [showCreate, setShowCreate] = useState(false);
    const [accountName, setAccountName] = useState('');
    const [seedMoney, setSeedMoney] = useState(10000000);
    const [creating, setCreating] = useState(false);

    // AI 리포트
    const [reportOpen, setReportOpen] = useState(false);
    const [reportLoading, setReportLoading] = useState(false);
    const [reportContent, setReportContent] = useState('');

    const fetchData = useCallback(async () => {
        if (!userId) return;
        setLoading(true);
        try {
            const [stocksRes, accRes] = await Promise.all([
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
            if (stocksRes.ok) setStocks(await stocksRes.json() || []);
            if (accRes.ok) {
                const acc = await accRes.json();
                setAccount(acc?.id ? acc : null);
            }
        } catch (e) {
            console.error(e);
        } finally {
            setLoading(false);
        }
    }, [userId]);

    useEffect(() => {
        fetchData();
    }, [fetchData]);

    const handleCreateAccount = async () => {
        if (!accountName.trim()) { toast.error('계좌 이름을 입력해주세요'); return; }
        setCreating(true);
        try {
            const res = await fetch(`${BASE_URL}/account`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ name: accountName, deposit: seedMoney, seedMoney, customerId: userId }),
            });
            if (res.ok) {
                toast.success('계좌가 생성되었습니다!');
                setShowCreate(false);
                setAccountName('');
                fetchData();
            } else {
                toast.error('계좌 생성 중 오류가 발생했습니다');
            }
        } catch {
            toast.error('계좌 생성 중 오류가 발생했습니다');
        } finally {
            setCreating(false);
        }
    };

    const handleAiReport = async () => {
        setReportOpen(true);
        setReportLoading(true);
        setReportContent('');
        try {
            const res = await fetch(`${BASE_URL}/chatbot/portfolio-report`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ customerId: userId }),
            });
            if (res.ok) {
                const data = await res.json();
                setReportContent(data.report);
            } else {
                setReportContent('리포트 생성에 실패했습니다.');
            }
        } catch {
            setReportContent('서버 연결에 실패했습니다.');
        } finally {
            setReportLoading(false);
        }
    };

    const totalBuy = stocks.reduce((s, st) => s + st.totalBuyPrice, 0);
    const totalNow = stocks.reduce((s, st) => s + st.nowPrice * st.amount, 0);
    const profit = totalNow - totalBuy;
    const profitRate = totalBuy > 0 ? (profit / totalBuy * 100).toFixed(2) : '0.00';
    const isProfit = profit >= 0;
    const totalAsset = totalNow + (account?.deposit || 0);
    const returnOnSeed = account?.seedMoney > 0
        ? ((totalAsset - account.seedMoney) / account.seedMoney * 100).toFixed(2)
        : '0.00';

    const fmt = (n) => (n || 0).toLocaleString('ko-KR');

    const growthStage = GROWTH_STAGES.find(
        s => parseFloat(returnOnSeed) >= s.min && parseFloat(returnOnSeed) < s.max
    ) || GROWTH_STAGES[0];

    const pieData = [
        ...stocks.map((st, i) => ({
            name: st.name,
            value: st.nowPrice * st.amount,
            color: PIE_COLORS[i % PIE_COLORS.length],
        })),
        account?.deposit > 0
            ? { name: '예수금', value: account.deposit, color: '#636e72' }
            : null,
    ].filter(Boolean);

    if (!isLoggedIn) return <div className={classes.notLogin}>로그인이 필요합니다</div>;

    // 계좌 없을 때: 계좌 생성 화면
    if (!loading && !account) {
        return (
            <div className={classes.page}>
                <div className={classes.noAccountWrap}>
                    <IconCreditCardFilled size={48} stroke={1.5} className={classes.noAccountIcon} />
                    <h2 className={classes.noAccountTitle}>모의투자 계좌가 없습니다</h2>
                    <p className={classes.noAccountDesc}>계좌를 만들고 모의투자를 시작해보세요</p>

                    {!showCreate ? (
                        <button className={classes.createBtn} onClick={() => setShowCreate(true)}>
                            계좌 만들기
                        </button>
                    ) : (
                        <div className={classes.createForm}>
                            <div className={classes.formGroup}>
                                <label className={classes.formLabel}>계좌명</label>
                                <input
                                    className={classes.formInput}
                                    placeholder="예: 모의투자 계좌"
                                    value={accountName}
                                    onChange={e => setAccountName(e.target.value)}
                                    onKeyDown={e => e.key === 'Enter' && handleCreateAccount()}
                                />
                            </div>
                            <div className={classes.formGroup}>
                                <label className={classes.formLabel}>시드머니 (초기 예수금)</label>
                                <div className={classes.seedGrid}>
                                    {SEED_OPTIONS.map(opt => (
                                        <button
                                            key={opt.value}
                                            className={`${classes.seedBtn} ${seedMoney === opt.value ? classes.seedActive : ''}`}
                                            onClick={() => setSeedMoney(opt.value)}
                                        >
                                            {opt.label}
                                        </button>
                                    ))}
                                </div>
                                <div className={classes.seedDisplay}>{fmt(seedMoney)}원</div>
                            </div>
                            <div className={classes.formActions}>
                                <button className={classes.cancelBtn} onClick={() => setShowCreate(false)}>취소</button>
                                <button className={classes.confirmBtn} onClick={handleCreateAccount} disabled={creating}>
                                    {creating ? '생성 중...' : '계좌 생성'}
                                </button>
                            </div>
                        </div>
                    )}
                </div>
            </div>
        );
    }

    return (
        <div className={classes.page}>
            {/* 성장 단계 */}
            <div className={classes.growthCard}>
                <div className={classes.growthIconWrap}>
                    <growthStage.Icon size={28} stroke={1.5} />
                </div>
                <div className={classes.growthInfo}>
                    <span className={classes.growthLabel}>{growthStage.label}</span>
                    <span className={classes.growthDesc}>{growthStage.desc}</span>
                </div>
                <div className={classes.growthPath}>
                    {GROWTH_STAGES.map((s, i) => (
                        <span
                            key={i}
                            className={`${classes.growthDot} ${s === growthStage ? classes.growthDotActive : ''}`}
                            title={s.label}
                        >
                            <s.Icon size={16} stroke={1.5} />
                        </span>
                    ))}
                </div>
            </div>

            {/* 총 자산 카드 */}
            <div className={classes.assetCard}>
                <div className={classes.assetTopRow}>
                    <div>
                        <span className={classes.assetLabel}>{account?.name || '모의투자 계좌'}</span>
                        <div className={classes.assetValue}>{fmt(totalAsset)}원</div>
                    </div>
                    <div className={classes.returnBadge}>
                        <span className={classes.returnLabel}>시드 대비</span>
                        <span className={`${classes.returnRate} ${parseFloat(returnOnSeed) >= 0 ? classes.rise : classes.fall}`}>
                            {parseFloat(returnOnSeed) >= 0 ? '+' : ''}{returnOnSeed}%
                        </span>
                    </div>
                </div>
                <div className={`${classes.profitSummary} ${isProfit ? classes.rise : classes.fall}`}>
                    {isProfit ? <IconTrendingUp size={16} stroke={1.5} /> : <IconTrendingDown size={16} stroke={1.5} />}
                    <span>평가손익 {isProfit ? '+' : ''}{fmt(profit)}원</span>
                    <span className={classes.profitRateText}>({isProfit ? '+' : ''}{profitRate}%)</span>
                </div>
                <div className={classes.assetStats}>
                    <div className={classes.assetStat}>
                        <span>총 매입금액</span>
                        <strong>{fmt(totalBuy)}원</strong>
                    </div>
                    <div className={classes.assetStat}>
                        <span>총 평가금액</span>
                        <strong>{fmt(totalNow)}원</strong>
                    </div>
                    <div className={classes.assetStat}>
                        <span>예수금</span>
                        <strong>{fmt(account?.deposit)}원</strong>
                    </div>
                    <div className={classes.assetStat}>
                        <span>시드머니</span>
                        <strong>{fmt(account?.seedMoney)}원</strong>
                    </div>
                </div>
            </div>

            {/* AI 리포트 버튼 */}
            <button className={classes.aiReportBtn} onClick={handleAiReport} disabled={reportLoading}>
                <IconSparkles size={18} stroke={1.5} />
                {reportLoading ? 'AI 리포트 생성 중...' : 'AI 포트폴리오 리포트'}
            </button>

            {/* 자산 배분 파이차트 */}
            {pieData.length > 0 && (
                <div className={classes.section}>
                    <h2 className={classes.sectionTitle}>자산 배분</h2>
                    <div className={classes.pieArea}>
                        <div className={classes.pieChart}>
                            <PieChart width={160} height={160}>
                                <Pie
                                    data={pieData}
                                    cx="50%"
                                    cy="50%"
                                    innerRadius={45}
                                    outerRadius={70}
                                    paddingAngle={2}
                                    dataKey="value"
                                >
                                    {pieData.map((entry, i) => (
                                        <Cell key={i} fill={entry.color} />
                                    ))}
                                </Pie>
                                <Tooltip
                                    formatter={(v) => `${v.toLocaleString()}원`}
                                    contentStyle={{ background: '#FFFDF7', border: '1px solid #EDE5D0', borderRadius: 8, fontSize: 12, boxShadow: '0 4px 12px rgba(234,179,8,0.1)' }}
                                    itemStyle={{ color: '#1C1300' }}
                                    labelStyle={{ color: '#6B5A32' }}
                                />
                            </PieChart>
                        </div>
                        <div className={classes.legend}>
                            {pieData.map((d, i) => (
                                <div key={i} className={classes.legendItem}>
                                    <span className={classes.legendDot} style={{ background: d.color }} />
                                    <span className={classes.legendName}>{d.name}</span>
                                    <span className={classes.legendPct}>
                                        {totalAsset > 0 ? (d.value / totalAsset * 100).toFixed(1) : 0}%
                                    </span>
                                </div>
                            ))}
                        </div>
                    </div>
                </div>
            )}

            {/* 보유종목 리스트 */}
            <div className={classes.section}>
                <h2 className={classes.sectionTitle}>
                    보유종목 {stocks.length > 0 && <span className={classes.count}>{stocks.length}</span>}
                </h2>
                {loading ? (
                    <div className={classes.loading}>로딩 중...</div>
                ) : stocks.length === 0 ? (
                    <div className={classes.empty}>보유 중인 종목이 없습니다</div>
                ) : (
                    <div className={classes.stockList}>
                        {stocks
                            .slice()
                            .sort((a, b) => {
                                const pa = (a.nowPrice * a.amount - a.totalBuyPrice) / (a.totalBuyPrice || 1);
                                const pb = (b.nowPrice * b.amount - b.totalBuyPrice) / (b.totalBuyPrice || 1);
                                return pb - pa;
                            })
                            .map(stock => {
                                const nowTotal = stock.nowPrice * stock.amount;
                                const stockProfit = nowTotal - stock.totalBuyPrice;
                                const stockRate = stock.totalBuyPrice > 0
                                    ? (stockProfit / stock.totalBuyPrice * 100).toFixed(2)
                                    : '0.00';
                                const isUp = stockProfit >= 0;
                                return (
                                    <div
                                        key={stock.stockCode}
                                        className={classes.stockCard}
                                        onClick={() => navigate(`/stock/${stock.stockCode}`)}
                                    >
                                        <div className={classes.stockTop}>
                                            <div>
                                                <span className={classes.stockName}>{stock.name}</span>
                                                <span className={classes.stockCode}>{stock.stockCode}</span>
                                            </div>
                                            <div className={classes.stockRight}>
                                                <span className={classes.nowPriceLabel}>{fmt(nowTotal)}원</span>
                                                <span className={`${classes.profitRate} ${isUp ? classes.rise : classes.fall}`}>
                                                    {isUp ? <IconTrendingUp size={16} stroke={1.5} /> : <IconTrendingDown size={16} stroke={1.5} />}
                                                    {isUp ? '+' : ''}{stockRate}%
                                                </span>
                                            </div>
                                        </div>
                                        <div className={classes.stockBottom}>
                                            <span className={classes.detail}>
                                                {stock.amount}주 · 평균 {fmt(Math.round(stock.totalBuyPrice / stock.amount))}원
                                            </span>
                                            <span className={`${classes.profitAmt} ${isUp ? classes.rise : classes.fall}`}>
                                                {isUp ? '+' : ''}{fmt(stockProfit)}원
                                            </span>
                                        </div>
                                        <div className={classes.progressBar}>
                                            <div
                                                className={`${classes.progressFill} ${isUp ? classes.riseBar : classes.fallBar}`}
                                                style={{ width: `${Math.min(100, Math.abs(parseFloat(stockRate)) * 5)}%` }}
                                            />
                                        </div>
                                    </div>
                                );
                            })}
                    </div>
                )}
            </div>
            {/* AI 리포트 모달 */}
            {reportOpen && (
                <div className={classes.modalOverlay} onClick={() => setReportOpen(false)}>
                    <div className={classes.reportModal} onClick={e => e.stopPropagation()}>
                        <div className={classes.reportHeader}>
                            <div className={classes.reportTitleRow}>
                                <IconSparkles size={20} stroke={1.5} style={{ color: 'var(--accent)' }} />
                                <h3 className={classes.reportTitle}>AI 포트폴리오 리포트</h3>
                            </div>
                            <button className={classes.reportClose} onClick={() => setReportOpen(false)}>
                                <IconX size={20} stroke={1.5} />
                            </button>
                        </div>
                        <div className={classes.reportBody}>
                            {reportLoading ? (
                                <div className={classes.reportLoading}>
                                    <div className={classes.spinner} />
                                    <p>AI가 포트폴리오를 분석하고 있습니다...</p>
                                </div>
                            ) : (
                                <div className={classes.reportContent}>
                                    <Markdown>{reportContent}</Markdown>
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default PortfolioPage;

import React, { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useApp } from '../../context/AppContext';
import toast from 'react-hot-toast';
import { IconBuildingBank, IconLogout, IconTrash, IconCreditCardFilled, IconRefresh, IconEgg, IconEggCracked } from '@tabler/icons-react';
import { IconChick, IconChicken } from '../../components/icons/GrowthIcons';
import { API_BASE_URL } from '../../config/api';
import classes from './MyPage.module.css';

const BASE_URL = API_BASE_URL;

const MyPage = () => {
    const { userId, userName, isLoggedIn, logout } = useApp();
    const navigate = useNavigate();
    const [customer, setCustomer] = useState(null);
    const [account, setAccount] = useState(null);
    const [myStocks, setMyStocks] = useState([]);
    const [showCreateAccount, setShowCreateAccount] = useState(false);
    const [newAccountName, setNewAccountName] = useState('');
    const [seedMoney, setSeedMoney] = useState('10000000');
    const [loading, setLoading] = useState(false);

    const fetchData = useCallback(async () => {
        if (!userId) return;
        try {
            const [custRes, accRes, stockRes] = await Promise.all([
                fetch(`${BASE_URL}/info/customer`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ id: userId }),
                }),
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
            if (custRes.ok) setCustomer(await custRes.json());
            if (accRes.ok) setAccount(await accRes.json());
            if (stockRes.ok) setMyStocks(await stockRes.json() || []);
        } catch (e) {
            console.error(e);
        }
    }, [userId]);

    useEffect(() => {
        if (!isLoggedIn) { navigate('/'); return; }
        fetchData();
    }, [isLoggedIn, fetchData, navigate]);

    const handleLogout = () => {
        logout();
        toast.success('로그아웃 되었습니다');
        navigate('/');
    };

    const handleCreateAccount = async () => {
        if (!newAccountName.trim()) { toast.error('계좌 이름을 입력해주세요'); return; }
        setLoading(true);
        try {
            const seed = parseInt(seedMoney.replace(/,/g, '')) || 10000000;
            const res = await fetch(`${BASE_URL}/account`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ name: newAccountName, deposit: seed, seedMoney: seed, customerId: userId }),
            });
            if (res.ok) {
                toast.success('계좌가 생성되었습니다!');
                setShowCreateAccount(false);
                setNewAccountName('');
                fetchData();
            } else {
                toast.error('계좌 생성 중 오류가 발생했습니다');
            }
        } catch {
            toast.error('계좌 생성 중 오류가 발생했습니다');
        } finally {
            setLoading(false);
        }
    };

    const handleResetAccount = async () => {
        if (!window.confirm('계좌를 초기화하시겠습니까?\n보유 종목과 거래내역이 모두 삭제되고 예수금이 시드머니로 초기화됩니다.')) return;
        try {
            const res = await fetch(`${BASE_URL}/account/reset`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ customerId: userId }),
            });
            if (res.ok) {
                toast.success('계좌가 초기화되었습니다');
                fetchData();
            } else {
                toast.error('초기화 중 오류가 발생했습니다');
            }
        } catch {
            toast.error('초기화 중 오류가 발생했습니다');
        }
    };

    const handleDeleteAccount = async () => {
        if (!window.confirm('계좌를 삭제하시겠습니까? 모든 보유 종목도 삭제됩니다.')) return;
        try {
            const res = await fetch(`${BASE_URL}/info/account`, {
                method: 'DELETE',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ id: account.id, customerId: userId }),
            });
            if (res.ok) {
                toast.success('계좌가 삭제되었습니다');
                setAccount(null);
                fetchData();
            }
        } catch {
            toast.error('삭제 중 오류가 발생했습니다');
        }
    };

    const fmt = (n) => (n || 0).toLocaleString('ko-KR');

    const totalNow = myStocks.reduce((s, st) => s + st.nowPrice * st.amount, 0);
    const totalBuy = myStocks.reduce((s, st) => s + st.totalBuyPrice, 0);
    const evalProfit = totalNow - totalBuy;
    const evalRate = totalBuy > 0 ? (evalProfit / totalBuy * 100).toFixed(2) : '0.00';
    const totalAsset = totalNow + (account?.deposit || 0);
    const seedReturn = account?.seedMoney > 0
        ? ((totalAsset - account.seedMoney) / account.seedMoney * 100).toFixed(2)
        : '0.00';
    const isProfit = evalProfit >= 0;

    const sr = parseFloat(seedReturn);
    const GrowthIcon = sr < 0 ? IconEgg : sr < 10 ? IconEggCracked : sr < 50 ? IconChick : IconChicken;
    const growthLabel = sr < 0 ? '알 단계' : sr < 10 ? '부화 단계' : sr < 50 ? '병아리 단계' : '닭 단계';

    if (!isLoggedIn) return null;

    return (
        <div className={classes.page}>
            {/* 프로필 헤더 */}
            <div className={classes.profileSection}>
                <div className={classes.avatar}>
                    <GrowthIcon size={26} stroke={1.5} className={classes.avatarIcon} />
                </div>
                <div className={classes.profileInfo}>
                    <h2 className={classes.profileName}>{customer?.name || userName}</h2>
                    <span className={classes.profileEmail}>{customer?.email}</span>
                    {account && (
                        <span className={classes.growthBadge}>
                            <GrowthIcon size={14} stroke={1.5} />
                            {growthLabel}
                        </span>
                    )}
                </div>
                <button className={classes.logoutBtn} onClick={handleLogout}>
                    <IconLogout size={18} stroke={1.5} />
                </button>
            </div>

            {/* 투자 성과 요약 */}
            {account && (
                <div className={classes.performanceCard}>
                    <div className={classes.perfRow}>
                        <span className={classes.perfLabel}>총 자산</span>
                        <span className={classes.perfMain}>{fmt(totalAsset)}원</span>
                    </div>
                    <div className={classes.perfDivider} />
                    <div className={classes.perfGrid}>
                        <div className={classes.perfItem}>
                            <span className={classes.perfItemLabel}>시드 대비 수익률</span>
                            <span className={`${classes.perfItemValue} ${parseFloat(seedReturn) >= 0 ? classes.rise : classes.fall}`}>
                                {parseFloat(seedReturn) >= 0 ? '+' : ''}{seedReturn}%
                            </span>
                        </div>
                        <div className={classes.perfItem}>
                            <span className={classes.perfItemLabel}>평가손익</span>
                            <span className={`${classes.perfItemValue} ${isProfit ? classes.rise : classes.fall}`}>
                                {isProfit ? '+' : ''}{fmt(evalProfit)}원
                            </span>
                        </div>
                        <div className={classes.perfItem}>
                            <span className={classes.perfItemLabel}>보유종목</span>
                            <span className={classes.perfItemValue}>{myStocks.length}종목</span>
                        </div>
                        <div className={classes.perfItem}>
                            <span className={classes.perfItemLabel}>평가수익률</span>
                            <span className={`${classes.perfItemValue} ${isProfit ? classes.rise : classes.fall}`}>
                                {isProfit ? '+' : ''}{evalRate}%
                            </span>
                        </div>
                    </div>
                </div>
            )}

            {/* 계좌 정보 */}
            <div className={classes.section}>
                <div className={classes.sectionHeader}>
                    <IconBuildingBank size={18} stroke={1.5} className={classes.sectionIcon} />
                    <h3 className={classes.sectionTitle}>계좌 정보</h3>
                </div>
                {account ? (
                    <div className={classes.accountCard}>
                        <div className={classes.accountRow}>
                            <span className={classes.accountLabel}>계좌명</span>
                            <span className={classes.accountValue}>{account.name}</span>
                        </div>
                        <div className={classes.accountRow}>
                            <span className={classes.accountLabel}>예수금</span>
                            <span className={classes.accountValue}>{fmt(account.deposit)}원</span>
                        </div>
                        <div className={classes.accountRow}>
                            <span className={classes.accountLabel}>시드머니</span>
                            <span className={classes.accountValue}>{fmt(account.seedMoney)}원</span>
                        </div>
                        <div className={classes.accountActions}>
                            <button className={classes.resetBtn} onClick={handleResetAccount}>
                                <IconRefresh size={16} stroke={1.5} />
                                계좌 초기화
                            </button>
                            <button className={classes.deleteBtn} onClick={handleDeleteAccount}>
                                <IconTrash size={16} stroke={1.5} />
                                계좌 삭제
                            </button>
                        </div>
                    </div>
                ) : (
                    <div className={classes.noAccount}>
                        {!showCreateAccount ? (
                            <>
                                <p>등록된 계좌가 없습니다</p>
                                <button className={classes.createBtn} onClick={() => setShowCreateAccount(true)}>
                                    <IconCreditCardFilled size={16} stroke={1.5} />
                                    계좌 만들기
                                </button>
                            </>
                        ) : (
                            <div className={classes.createForm}>
                                <input
                                    className={classes.input}
                                    placeholder="계좌 이름 (예: 모의투자 계좌)"
                                    value={newAccountName}
                                    onChange={e => setNewAccountName(e.target.value)}
                                />
                                <div className={classes.seedOptions}>
                                    <span className={classes.seedLabel}>시드머니</span>
                                    <div className={classes.seedBtns}>
                                        {['1000000', '5000000', '10000000', '50000000'].map(v => (
                                            <button
                                                key={v}
                                                className={`${classes.seedBtn} ${seedMoney === v ? classes.activeSeed : ''}`}
                                                onClick={() => setSeedMoney(v)}
                                            >
                                                {parseInt(v).toLocaleString('ko-KR')}원
                                            </button>
                                        ))}
                                    </div>
                                </div>
                                <div className={classes.formActions}>
                                    <button className={classes.cancelBtn} onClick={() => setShowCreateAccount(false)}>취소</button>
                                    <button className={classes.confirmBtn} onClick={handleCreateAccount} disabled={loading}>
                                        {loading ? '생성 중...' : '계좌 생성'}
                                    </button>
                                </div>
                            </div>
                        )}
                    </div>
                )}
            </div>

            {/* 투자 수익률 리셋 안내 */}
            <div className={classes.notice}>
                <span>💡 모의투자 계좌는 최대 1개까지 생성 가능합니다</span>
            </div>
        </div>
    );
};

export default MyPage;

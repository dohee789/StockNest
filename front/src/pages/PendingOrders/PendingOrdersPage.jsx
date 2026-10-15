import React, { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useApp } from '../../context/AppContext';
import toast from 'react-hot-toast';
import ArrowBackRoundedIcon from '@mui/icons-material/ArrowBackRounded';
import PendingActionsRoundedIcon from '@mui/icons-material/PendingActionsRounded';
import CloseRoundedIcon from '@mui/icons-material/CloseRounded';
import { API_BASE_URL } from '../../config/api';
import classes from './PendingOrdersPage.module.css';

const BASE_URL = API_BASE_URL;

const STATUS_LABEL = { PENDING: '대기', EXECUTED: '체결', CANCELLED: '취소' };
const STATUS_CLASS = { PENDING: 'pending', EXECUTED: 'executed', CANCELLED: 'cancelled' };

const CONDITION_LABEL = {
    PRICE_BELOW: '이하 시',
    PRICE_ABOVE: '이상 시',
};

const ORDER_TYPE_LABEL = {
    'BUY+PRICE_BELOW': '지정가 매수',
    'SELL+PRICE_ABOVE': '익절 매도',
    'SELL+PRICE_BELOW': '손절 매도',
};

const PendingOrdersPage = () => {
    const { userId, isLoggedIn } = useApp();
    const navigate = useNavigate();
    const [orders, setOrders] = useState([]);
    const [loading, setLoading] = useState(false);
    const [filter, setFilter] = useState('PENDING');

    const fetchOrders = useCallback(async () => {
        if (!userId) return;
        setLoading(true);
        try {
            const res = await fetch(`${BASE_URL}/pending-order/list`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ customerId: userId }),
            });
            if (res.ok) setOrders(await res.json() || []);
        } catch (e) {
            console.error(e);
        } finally {
            setLoading(false);
        }
    }, [userId]);

    useEffect(() => {
        if (!isLoggedIn) { navigate('/'); return; }
        fetchOrders();
    }, [isLoggedIn, fetchOrders, navigate]);

    const handleCancel = async (orderId) => {
        if (!window.confirm('대기 주문을 취소하시겠습니까?')) return;
        try {
            const res = await fetch(`${BASE_URL}/pending-order/cancel`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ orderId, customerId: userId }),
            });
            if (res.ok) {
                toast.success('주문이 취소되었습니다');
                fetchOrders();
            } else {
                toast.error(await res.text());
            }
        } catch {
            toast.error('취소 중 오류가 발생했습니다');
        }
    };

    const fmt = (n) => (n || 0).toLocaleString('ko-KR');
    const formatDate = (str) => {
        if (!str) return '-';
        const d = new Date(str);
        return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
    };

    const filtered = orders.filter(o => filter === 'ALL' ? true : o.status === filter);
    const pendingCount = orders.filter(o => o.status === 'PENDING').length;

    if (!isLoggedIn) return null;

    return (
        <div className={classes.page}>
            <div className={classes.header}>
                <button className={classes.backBtn} onClick={() => navigate(-1)}>
                    <ArrowBackRoundedIcon />
                </button>
                <h1 className={classes.title}>대기주문</h1>
                {pendingCount > 0 && <span className={classes.badge}>{pendingCount}</span>}
            </div>

            {/* 안내 */}
            <div className={classes.notice}>
                <PendingActionsRoundedIcon fontSize="small" />
                <span>목표가 도달 시 30초 이내 자동 체결됩니다</span>
            </div>

            {/* 필터 */}
            <div className={classes.filters}>
                {[['PENDING', '대기'], ['EXECUTED', '체결'], ['CANCELLED', '취소'], ['ALL', '전체']].map(([val, label]) => (
                    <button
                        key={val}
                        className={`${classes.filterBtn} ${filter === val ? classes.filterActive : ''}`}
                        onClick={() => setFilter(val)}
                    >
                        {label}
                        {val === 'PENDING' && pendingCount > 0 && <span className={classes.filterBadge}>{pendingCount}</span>}
                    </button>
                ))}
            </div>

            {loading ? (
                <div className={classes.empty}>로딩 중...</div>
            ) : filtered.length === 0 ? (
                <div className={classes.empty}>
                    {filter === 'PENDING' ? '대기 중인 주문이 없습니다' : '주문 내역이 없습니다'}
                </div>
            ) : (
                <div className={classes.list}>
                    {filtered.map(order => {
                        const orderLabel = ORDER_TYPE_LABEL[`${order.orderType}+${order.conditionType}`]
                            || `${order.orderType} (${CONDITION_LABEL[order.conditionType]})`;
                        const isBuy = order.orderType === 'BUY';
                        return (
                            <div key={order.id} className={classes.card} onClick={() => navigate(`/stock/${order.stockCode}`)}>
                                <div className={classes.cardTop}>
                                    <div className={classes.cardLeft}>
                                        <div className={classes.orderTypeRow}>
                                            <span className={`${classes.orderType} ${isBuy ? classes.buyType : classes.sellType}`}>
                                                {orderLabel}
                                            </span>
                                            <span className={`${classes.statusBadge} ${classes[STATUS_CLASS[order.status]]}`}>
                                                {STATUS_LABEL[order.status]}
                                            </span>
                                        </div>
                                        <span className={classes.stockName}>{order.stockName}</span>
                                        <span className={classes.stockCode}>{order.stockCode}</span>
                                    </div>
                                    <div className={classes.cardRight}>
                                        <span className={classes.targetPrice}>{fmt(order.targetPrice)}원</span>
                                        <span className={classes.amount}>{order.amount}주</span>
                                        {order.status === 'PENDING' && (
                                            <button
                                                className={classes.cancelBtn}
                                                onClick={e => { e.stopPropagation(); handleCancel(order.id); }}
                                            >
                                                <CloseRoundedIcon fontSize="small" />
                                            </button>
                                        )}
                                    </div>
                                </div>
                                <div className={classes.cardBottom}>
                                    <span>등록: {formatDate(order.createdAt)}</span>
                                    {order.executedAt && <span>체결: {formatDate(order.executedAt)}</span>}
                                    <span>총 {fmt(order.targetPrice * order.amount)}원</span>
                                </div>
                            </div>
                        );
                    })}
                </div>
            )}
        </div>
    );
};

export default PendingOrdersPage;

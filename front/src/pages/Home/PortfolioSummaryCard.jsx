import React, { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useApp } from '../../context/AppContext';
import { IconChevronRight, IconEgg, IconEggCracked } from '@tabler/icons-react';
import { IconChick, IconChicken } from '../../components/icons/GrowthIcons';
import { API_BASE_URL } from '../../config/api';
import classes from './PortfolioSummaryCard.module.css';

const BASE_URL = API_BASE_URL;

const PortfolioSummaryCard = () => {
    const { userId } = useApp();
    const navigate = useNavigate();
    const [summary, setSummary] = useState({ totalBuy: 0, totalNow: 0, deposit: 0 });

    const fetchData = useCallback(async () => {
        if (!userId) return;
        try {
            const [stocksRes, accountRes] = await Promise.all([
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

            let totalBuy = 0, totalNow = 0, deposit = 0;

            if (stocksRes.ok) {
                const stocks = await stocksRes.json();
                stocks.forEach(s => {
                    totalBuy += s.totalBuyPrice;
                    totalNow += s.nowPrice * s.amount;
                });
            }

            if (accountRes.ok) {
                const acc = await accountRes.json();
                deposit = acc.deposit || 0;
            }

            setSummary({ totalBuy, totalNow, deposit });
        } catch (e) {
            console.error(e);
        }
    }, [userId]);

    useEffect(() => {
        fetchData();
    }, [fetchData]);

    const profit = summary.totalNow - summary.totalBuy;
    const profitRate = summary.totalBuy > 0 ? (profit / summary.totalBuy * 100).toFixed(2) : '0.00';
    const isProfit = profit >= 0;
    const rate = parseFloat(profitRate);
    const GrowthIcon = rate < 0 ? IconEgg : rate < 10 ? IconEggCracked : rate < 50 ? IconChick : IconChicken;

    const fmt = (n) => n.toLocaleString('ko-KR');

    return (
        <div className={classes.card} onClick={() => navigate('/portfolio')}>
            <div className={classes.header}>
                <div className={classes.growthIconWrap}>
                    <GrowthIcon size={18} stroke={1.5} />
                </div>
                <span className={classes.title}>내 계좌 요약</span>
                <IconChevronRight size={14} stroke={2} className={classes.arrow} />
            </div>
            <div className={classes.body}>
                <div className={classes.stat}>
                    <span className={classes.statLabel}>총 수익</span>
                    <span className={`${classes.statValue} ${isProfit ? classes.rise : classes.fall}`}>
                        {isProfit ? '+' : ''}{fmt(profit)}원
                    </span>
                </div>
                <div className={classes.divider} />
                <div className={classes.stat}>
                    <span className={classes.statLabel}>수익률</span>
                    <span className={`${classes.statValue} ${isProfit ? classes.rise : classes.fall}`}>
                        {isProfit ? '+' : ''}{profitRate}%
                    </span>
                </div>
                <div className={classes.divider} />
                <div className={classes.stat}>
                    <span className={classes.statLabel}>예수금</span>
                    <span className={classes.statValue}>{fmt(summary.deposit)}원</span>
                </div>
            </div>
        </div>
    );
};

export default PortfolioSummaryCard;

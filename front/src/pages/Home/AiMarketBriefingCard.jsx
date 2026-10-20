import React, { useState, useEffect, useCallback } from 'react';
import { useApp } from '../../context/AppContext';
import { IconNews, IconRefresh, IconChevronDown, IconChevronUp } from '@tabler/icons-react';
import Markdown from 'markdown-to-jsx';
import { API_BASE_URL } from '../../config/api';
import classes from './AiMarketBriefingCard.module.css';

const CACHE_KEY = 'ai_briefing_cache';

const getCached = () => {
    try {
        const raw = sessionStorage.getItem(CACHE_KEY);
        if (!raw) return null;
        const { date, briefing } = JSON.parse(raw);
        if (date === new Date().toDateString()) return briefing;
    } catch { /* ignore */ }
    return null;
};

const setCache = (briefing) => {
    try {
        sessionStorage.setItem(CACHE_KEY, JSON.stringify({
            date: new Date().toDateString(),
            briefing,
        }));
    } catch { /* ignore */ }
};

const AiMarketBriefingCard = () => {
    const { userId } = useApp();
    const [briefing, setBriefing] = useState(getCached);
    const [loading, setLoading] = useState(false);
    const [expanded, setExpanded] = useState(false);
    const [error, setError] = useState(null);

    const fetchBriefing = useCallback(async () => {
        if (!userId) return;
        setLoading(true);
        setError(null);
        try {
            const res = await fetch(`${API_BASE_URL}/chatbot/market-briefing`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ customerId: userId }),
            });
            if (res.ok) {
                const data = await res.json();
                const text = data.briefing || '';
                setBriefing(text);
                setCache(text);
            } else {
                setError('브리핑을 불러올 수 없습니다');
            }
        } catch {
            setError('AI 서비스 연결에 실패했습니다');
        } finally {
            setLoading(false);
        }
    }, [userId]);

    useEffect(() => {
        if (!briefing) fetchBriefing();
    }, [briefing, fetchBriefing]);

    const now = new Date();
    const timeLabel = `${now.getMonth() + 1}/${now.getDate()} ${now.getHours()}:${String(now.getMinutes()).padStart(2, '0')} 기준`;

    return (
        <section className={classes.card}>
            <div className={classes.header}>
                <div className={classes.headerLeft}>
                    <IconNews size={18} stroke={1.5} className={classes.headerIcon} />
                    <h3 className={classes.title}>AI 시장 브리핑</h3>
                    <span className={classes.time}>{timeLabel}</span>
                </div>
                <div className={classes.headerActions}>
                    <button
                        className={classes.refreshBtn}
                        onClick={fetchBriefing}
                        disabled={loading}
                        title="새로고침"
                    >
                        <IconRefresh size={14} stroke={1.5} className={loading ? classes.spin : ''} />
                    </button>
                    {briefing && (
                        <button
                            className={classes.toggleBtn}
                            onClick={() => setExpanded(prev => !prev)}
                        >
                            {expanded
                                ? <IconChevronUp size={16} stroke={1.5} />
                                : <IconChevronDown size={16} stroke={1.5} />
                            }
                        </button>
                    )}
                </div>
            </div>

            {loading && !briefing && (
                <div className={classes.skeleton}>
                    <div className={classes.skeletonLine} style={{ width: '90%' }} />
                    <div className={classes.skeletonLine} style={{ width: '75%' }} />
                    <div className={classes.skeletonLine} style={{ width: '85%' }} />
                </div>
            )}

            {error && !loading && !briefing && (
                <div className={classes.error}>
                    <span>{error}</span>
                    <button onClick={fetchBriefing}>다시 시도</button>
                </div>
            )}

            {briefing && (
                <div className={`${classes.content} ${expanded ? classes.expanded : classes.collapsed}`}>
                    <Markdown options={{ forceBlock: true }}>{briefing}</Markdown>
                </div>
            )}

            {briefing && !expanded && (
                <button className={classes.expandBtn} onClick={() => setExpanded(true)}>
                    더보기
                </button>
            )}
        </section>
    );
};

export default AiMarketBriefingCard;

import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useApp } from '../../context/AppContext';
import { IconSparkles, IconRefresh, IconChevronRight } from '@tabler/icons-react';
import { API_BASE_URL } from '../../config/api';
import classes from './AiRecommendationsCard.module.css';

const CONFIDENCE_MAP = {
    high: { label: '높음', cls: 'high' },
    medium: { label: '보통', cls: 'medium' },
    low: { label: '낮음', cls: 'low' },
};

const AiRecommendationsCard = () => {
    const navigate = useNavigate();
    const { userId } = useApp();
    const [recommendations, setRecommendations] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    const fetchRecommendations = useCallback(async () => {
        if (!userId) return;
        setLoading(true);
        setError(null);
        try {
            const res = await fetch(`${API_BASE_URL}/chatbot/stock-recommendations`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ customerId: userId }),
            });
            if (res.ok) {
                const data = await res.json();
                setRecommendations(data.recommendations || []);
            } else {
                setError('추천을 불러올 수 없습니다');
            }
        } catch {
            setError('AI 서비스 연결에 실패했습니다');
        } finally {
            setLoading(false);
        }
    }, [userId]);

    useEffect(() => {
        fetchRecommendations();
    }, [fetchRecommendations]);

    return (
        <section className={classes.card}>
            <div className={classes.header}>
                <div className={classes.headerLeft}>
                    <IconSparkles size={18} stroke={1.5} className={classes.headerIcon} />
                    <h3 className={classes.title}>AI 맞춤 추천</h3>
                </div>
                <button
                    className={classes.refreshBtn}
                    onClick={fetchRecommendations}
                    disabled={loading}
                    title="새로고침"
                >
                    <IconRefresh size={16} stroke={1.5} className={loading ? classes.spin : ''} />
                </button>
            </div>

            {loading && !recommendations && (
                <div className={classes.skeleton}>
                    {[1, 2, 3].map(i => <div key={i} className={classes.skeletonItem} />)}
                </div>
            )}

            {error && !loading && (
                <div className={classes.error}>
                    <p>{error}</p>
                    <button onClick={fetchRecommendations}>다시 시도</button>
                </div>
            )}

            {recommendations && recommendations.length > 0 && (
                <div className={classes.list}>
                    {recommendations.map((rec, i) => {
                        const conf = CONFIDENCE_MAP[rec.confidence] || CONFIDENCE_MAP.medium;
                        return (
                            <button
                                key={rec.stockCode || i}
                                className={classes.item}
                                onClick={() => rec.stockCode && navigate(`/stock/${rec.stockCode}`)}
                            >
                                <div className={classes.itemTop}>
                                    <span className={classes.stockName}>{rec.stockName}</span>
                                    <span className={`${classes.badge} ${classes[conf.cls]}`}>
                                        {conf.label}
                                    </span>
                                </div>
                                <p className={classes.reason}>{rec.reason}</p>
                                <div className={classes.itemBottom}>
                                    <span className={classes.action}>
                                        {rec.action === 'buy' ? '매수 추천' : '관심 종목'}
                                    </span>
                                    <IconChevronRight size={14} stroke={1.5} />
                                </div>
                            </button>
                        );
                    })}
                </div>
            )}

            {recommendations && recommendations.length === 0 && !loading && (
                <div className={classes.empty}>추천 종목이 없습니다</div>
            )}
        </section>
    );
};

export default AiRecommendationsCard;

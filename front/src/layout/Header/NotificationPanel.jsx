import React, { useState } from 'react';
import { IconSparkles, IconX, IconTrash } from '@tabler/icons-react';
import Markdown from 'markdown-to-jsx';
import { API_BASE_URL } from '../../config/api';
import classes from './NotificationPanel.module.css';

const BASE_URL = API_BASE_URL;

const TYPE_LABEL = {
    BUY_MARKET: '시장가 매수',
    SELL_MARKET: '시장가 매도',
    BUY_LIMIT: '지정가 매수',
    SELL_LIMIT: '지정가 매도',
    DAILY_SUMMARY: '일일 결산',
    AI_INSIGHT: 'AI 인사이트',
};

const TYPE_COLOR = {
    BUY_MARKET: 'var(--rise)',
    SELL_MARKET: 'var(--fall)',
    BUY_LIMIT: 'var(--rise)',
    SELL_LIMIT: 'var(--fall)',
    DAILY_SUMMARY: 'var(--accent)',
    AI_INSIGHT: '#667eea',
};

function formatTime(isoStr) {
    if (!isoStr) return '';
    const d = new Date(isoStr);
    const pad = n => String(n).padStart(2, '0');
    return `${d.getMonth() + 1}/${d.getDate()} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

const NotificationPanel = ({ notifications, onMarkAllRead, onDelete, onDeleteAll, onClose, userId, onRefresh }) => {
    const [insightLoading, setInsightLoading] = useState(false);

    const handleGenerateInsight = async () => {
        if (!userId || insightLoading) return;
        setInsightLoading(true);
        try {
            const res = await fetch(`${BASE_URL}/notifications/${userId}/ai-insight`, { method: 'POST' });
            if (res.ok && onRefresh) onRefresh();
        } catch { /* ignore */ }
        finally { setInsightLoading(false); }
    };

    return (
        <div className={classes.panel}>
            <div className={classes.panelHeader}>
                <span className={classes.panelTitle}>알림</span>
                <div className={classes.headerRight}>
                    <button
                        className={classes.insightBtn}
                        onClick={handleGenerateInsight}
                        disabled={insightLoading}
                        title="AI 마켓 인사이트 생성"
                    >
                        <IconSparkles size={14} stroke={1.5} />
                        {insightLoading ? '생성 중...' : 'AI 인사이트'}
                    </button>
                    <button className={classes.markReadBtn} onClick={onMarkAllRead}>모두 읽음</button>
                    {notifications.length > 0 && (
                        <button className={classes.deleteAllBtn} onClick={onDeleteAll} title="모두 삭제">
                            <IconTrash size={14} stroke={1.5} />
                        </button>
                    )}
                </div>
            </div>
            <div className={classes.list}>
                {notifications.length === 0 ? (
                    <div className={classes.empty}>알림이 없습니다</div>
                ) : (
                    notifications.map(n => (
                        <div key={n.id} className={`${classes.item} ${n.read ? classes.read : ''}`}>
                            <div className={classes.itemTop}>
                                <span className={classes.tag} style={{ color: TYPE_COLOR[n.type] }}>
                                    {TYPE_LABEL[n.type] || n.type}
                                </span>
                                <div className={classes.itemActions}>
                                    <span className={classes.time}>{formatTime(n.createdAt)}</span>
                                    <button
                                        className={classes.deleteBtn}
                                        onClick={(e) => { e.stopPropagation(); onDelete(n.id); }}
                                        title="삭제"
                                    >
                                        <IconX size={12} stroke={2} />
                                    </button>
                                </div>
                            </div>
                            <div className={classes.title}>{n.title}</div>
                            <div className={classes.message}>
                                {n.type === 'AI_INSIGHT' ? (
                                    <div className={classes.markdown}>
                                        <Markdown>{n.message}</Markdown>
                                    </div>
                                ) : n.message}
                            </div>
                        </div>
                    ))
                )}
            </div>
        </div>
    );
};

export default NotificationPanel;

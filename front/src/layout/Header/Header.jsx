import React, { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useApp } from '../../context/AppContext';
import toast from 'react-hot-toast';
import classes from './Header.module.css';
import { IconHome, IconBell, IconHistory, IconMessageChatbot } from '@tabler/icons-react';
import NotificationPanel from './NotificationPanel';
import ChatPanel from './ChatPanel';

const Header = () => {
    const navigate = useNavigate();
    const { isLoggedIn, unreadCount, notifications, markAllRead, deleteNotification, deleteAllNotifications, fetchNotifications, userId } = useApp();
    const [panelOpen, setPanelOpen] = useState(false);
    const [chatOpen, setChatOpen] = useState(false);
    const panelRef = useRef(null);

    const handleChat = () => {
        if (!isLoggedIn) { toast.error('로그인이 필요합니다'); return; }
        setChatOpen(prev => !prev);
    };

    const handleHistory = () => {
        if (!isLoggedIn) { toast.error('로그인이 필요합니다'); return; }
        navigate('/history');
    };

    const handleNotification = () => {
        if (!isLoggedIn) { toast.error('로그인이 필요합니다'); return; }
        if (!panelOpen) {
            fetchNotifications(userId);
        }
        setPanelOpen(prev => !prev);
    };

    // 패널 외부 클릭 시 닫기
    useEffect(() => {
        if (!panelOpen) return;
        const handleClick = (e) => {
            if (panelRef.current && !panelRef.current.contains(e.target)) {
                setPanelOpen(false);
            }
        };
        document.addEventListener('mousedown', handleClick);
        return () => document.removeEventListener('mousedown', handleClick);
    }, [panelOpen]);

    return (
        <header className={classes.header}>
            <div className={classes.logo} onClick={() => navigate('/')}>
                <IconHome size={20} stroke={1.5} className={classes.logoIcon} />
                <span className={classes.logoText}>주린이집</span>
                <span className={classes.logoTag}>HTS</span>
            </div>
            <div className={classes.actions}>
                <button className={classes.iconBtn} onClick={handleChat} title="AI 챗봇">
                    <IconMessageChatbot size={20} stroke={1.5} />
                </button>
                <button className={classes.iconBtn} onClick={handleHistory} title="거래내역">
                    <IconHistory size={20} stroke={1.5} />
                </button>
                <div className={classes.bellWrapper} ref={panelRef}>
                    <button className={classes.iconBtn} onClick={handleNotification} title="알림">
                        <IconBell size={20} stroke={1.5} />
                        {isLoggedIn && unreadCount > 0 && (
                            <span className={classes.badge}>{unreadCount > 99 ? '99+' : unreadCount}</span>
                        )}
                    </button>
                    {panelOpen && (
                        <NotificationPanel
                            notifications={notifications}
                            onMarkAllRead={() => { markAllRead(); }}
                            onDelete={(id) => { deleteNotification(id); }}
                            onDeleteAll={() => { deleteAllNotifications(); }}
                            onClose={() => setPanelOpen(false)}
                            userId={userId}
                            onRefresh={() => fetchNotifications(userId)}
                        />
                    )}
                </div>
            </div>
            {chatOpen && <ChatPanel onClose={() => setChatOpen(false)} />}
        </header>
    );
};

export default Header;
